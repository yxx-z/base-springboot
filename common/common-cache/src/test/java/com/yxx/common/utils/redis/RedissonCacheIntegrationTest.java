package com.yxx.common.utils.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 Redis 验证缓存 Lua 脚本和并发语义。 */
@Testcontainers(disabledWithoutDocker = true)
class RedissonCacheIntegrationTest {

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);

    private static RedissonClient redissonClient;
    private static RedissonCache cache;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress(
                "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        redissonClient = Redisson.create(config);
        cache = new RedissonCache(redissonClient);
    }

    @AfterAll
    static void tearDown() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    void shouldKeepCounterTtlAndNeverCreateNegativeCounter() {
        String key = "integration:counter";
        assertEquals(1L, cache.increment(key, 60));
        assertTrue(redissonClient.getBucket(key).remainTimeToLive() > 0);
        assertEquals(0L, cache.decrement(key));
        assertFalse(cache.exists(key));

        assertEquals(0L, cache.decrement(key));
        assertFalse(cache.exists(key));
    }

    @Test
    void shouldAtomicallyLimitConcurrentLoginReservations() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(12);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int index = 0; index < 20; index++) {
                tasks.add(() -> cache.reserveLoginAttempt(
                        "integration:login:account", 5,
                        "integration:login:ip", 10, 60));
            }
            List<Future<Boolean>> futures = executor.invokeAll(tasks);
            long allowed = futures.stream().filter(this::result).count();
            assertEquals(5L, allowed);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReserveAndRestoreOneTimeStringWithoutLosingTtl() {
        String key = "integration:captcha";
        cache.putString(key, "123456", 60);
        assertEquals(1L, cache.reserveStringIfEquals(key, "123456", "reserved:test"));
        assertEquals(0L, cache.reserveStringIfEquals(key, "123456", "reserved:other"));

        cache.restoreReservedString(key, "reserved:test", "123456");
        assertTrue(redissonClient.getBucket(key).remainTimeToLive() > 0);
        assertEquals(1L, cache.reserveStringIfEquals(key, "123456", "reserved:commit"));
        cache.deleteStringIfEquals(key, "reserved:commit");
        assertFalse(cache.exists(key));
    }

    private boolean result(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new IllegalStateException("读取并发测试结果失败", exception);
        }
    }
}
