package com.yxx.common.utils.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 缓存访问组件。
 *
 * <p>连接生命周期完全交给 Redisson Spring Boot Starter 管理，禁止在工具类中重复创建客户端，
 * 避免一个应用维护两套连接池及关闭顺序不一致。</p>
 */
@Component
@RequiredArgsConstructor
public class RedissonCache {

    private static final long DEFAULT_EXPIRED_SECONDS = 5 * 60L;
    private static final String REDIS_KEY_PREFIX = "";

    private final RedissonClient redissonClient;

    public <T> T get(String key) {
        return this.<T>bucket(key).get();
    }

    public Boolean isExists(String key) {
        return bucket(key).isExists();
    }

    public String getString(String key) {
        return stringBucket(key).get();
    }

    public <T> void put(String key, T value) {
        this.<T>bucket(key).set(value, Duration.ofSeconds(DEFAULT_EXPIRED_SECONDS));
    }

    public void putString(String key, String value) {
        stringBucket(key).set(value, Duration.ofSeconds(DEFAULT_EXPIRED_SECONDS));
    }

    public void putString(String key, String value, long expiredSeconds) {
        stringBucket(key).set(value, duration(expiredSeconds, TimeUnit.SECONDS));
    }

    public void putString(String key, String value, long expired, TimeUnit timeUnit) {
        stringBucket(key).set(value, duration(expired, timeUnit));
    }

    public boolean putStringIfAbsent(String key, String value, long expiredSeconds) {
        return stringBucket(key).setIfAbsent(value, duration(expiredSeconds, TimeUnit.SECONDS));
    }

    public boolean putStringIfAbsent(String key, String value) {
        return stringBucket(key).setIfAbsent(value);
    }

    public <T> void put(String key, T value, long expiredSeconds) {
        this.<T>bucket(key).set(value, duration(expiredSeconds, TimeUnit.SECONDS));
    }

    public void remove(String key) {
        bucket(key).delete();
    }

    public boolean exists(String key) {
        return bucket(key).isExists();
    }

    /**
     * 原子递增计数器，并在首次创建时设置过期时间。
     *
     * @param key 计数器键
     * @param expiredSeconds 计数窗口秒数
     * @return 递增后的计数值
     */
    public long increment(String key, long expiredSeconds) {
        RAtomicLong counter = redissonClient.getAtomicLong(fullKey(key));
        long value = counter.incrementAndGet();
        if (value == 1L) {
            counter.expire(duration(expiredSeconds, TimeUnit.SECONDS));
        }
        return value;
    }

    /** 原子减少计数器，用于业务执行失败时归还已预占的次数。 */
    public long decrement(String key) {
        return redissonClient.getAtomicLong(fullKey(key)).decrementAndGet();
    }

    /** 读取原子计数器当前值；计数器不存在时返回 0。 */
    public long getCounterValue(String key) {
        return redissonClient.getAtomicLong(fullKey(key)).get();
    }

    public <T> RList<T> getRedisList(String key) {
        return redissonClient.getList(fullKey(key));
    }

    public <K, V> RMapCache<K, V> getRedisMap(String key) {
        return redissonClient.getMapCache(fullKey(key));
    }

    public <T> RSet<T> getRedisSet(String key) {
        return redissonClient.getSet(fullKey(key));
    }

    public <T> RScoredSortedSet<T> getRedisScoredSortedSet(String key) {
        return redissonClient.getScoredSortedSet(fullKey(key));
    }

    public RLock getRedisLock(String key) {
        return redissonClient.getLock(fullKey(key));
    }

    private <T> RBucket<T> bucket(String key) {
        return redissonClient.getBucket(fullKey(key));
    }

    private RBucket<String> stringBucket(String key) {
        return redissonClient.getBucket(fullKey(key), StringCodec.INSTANCE);
    }

    private String fullKey(String key) {
        return REDIS_KEY_PREFIX + key;
    }

    private Duration duration(long value, TimeUnit timeUnit) {
        if (value <= 0) {
            return Duration.ofSeconds(DEFAULT_EXPIRED_SECONDS);
        }
        return Duration.ofNanos(timeUnit.toNanos(value));
    }
}
