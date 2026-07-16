package com.yxx.common.utils.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
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

    public void putString(String key, String value, long expiredSeconds) {
        // 字符串场景显式使用 StringCodec，确保 Lua 脚本读取到的值与 Java 写入值编码一致。
        stringBucket(key).set(value, duration(expiredSeconds, TimeUnit.SECONDS));
    }

    public boolean putStringIfAbsent(String key, String value, long expiredSeconds) {
        // setIfAbsent 由 Redis 原子完成，适合一次性令牌预占、幂等锁等竞争场景。
        return stringBucket(key).setIfAbsent(value, duration(expiredSeconds, TimeUnit.SECONDS));
    }

    /**
     * 仅当当前字符串值与 expected 一致时替换为 reservation，并保留原 TTL。
     *
     * @return -1-Key 不存在，0-值不匹配，1-预占成功
     */
    public long reserveStringIfEquals(String key, String expected, String reservation) {
        // “读取、比较、替换”必须放在同一 Lua 脚本内，避免并发请求在比较后同时取得消费权。
        String script = "local current = redis.call('get', KEYS[1]); "
                + "if not current then return -1; end; "
                + "if current ~= ARGV[1] then return 0; end; "
                + "redis.call('set', KEYS[1], ARGV[2], 'KEEPTTL'); return 1;";
        // KEEPTTL 保留业务值原有的生命周期，预占动作不能意外延长验证码等敏感数据的有效期。
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(key)), expected, reservation);
        return result.longValue();
    }

    /** 仅当 Key 仍持有指定预占标记时删除。 */
    public void deleteStringIfEquals(String key, String expected) {
        // 条件删除可防止旧请求误删已经被其他流程替换的新值。
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then "
                + "return redis.call('del', KEYS[1]); end; return 0;";
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(key)), expected);
    }

    /** 事务回滚时仅在预占标记仍匹配的情况下恢复原值，并保留原 TTL。 */
    public void restoreReservedString(String key, String reservation, String originalValue) {
        // 仅预占所有者能够恢复原值，避免事务回滚覆盖后来写入的数据。
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then "
                + "redis.call('set', KEYS[1], ARGV[2], 'KEEPTTL'); return 1; end; return 0;";
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(key)), reservation, originalValue);
    }

    public <T> void put(String key, T value, long expiredSeconds) {
        // 泛型对象沿用 Redisson 客户端的统一序列化配置，调用方无需感知底层 Codec。
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
        // 递增与首次设置过期时间原子执行，避免进程在两条命令之间退出而留下永久计数器。
        String script = "local value = redis.call('incr', KEYS[1]); "
                + "if value == 1 then redis.call('expire', KEYS[1], ARGV[1]); end; "
                + "return value;";
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(key)), Math.max(1L, expiredSeconds));
        return result.longValue();
    }

    /**
     * 原子归还一次计数。Key 不存在时不会重新创建；归还后小于等于 0 时直接删除，避免形成
     * 没有 TTL 的永久负数计数器。
     */
    public long decrement(String key) {
        // 不对不存在的 Key 执行 DECR，否则 Redis 会创建一个没有 TTL 的负数键。
        String script = "if redis.call('exists', KEYS[1]) == 0 then return 0; end; "
                + "local value = redis.call('decr', KEYS[1]); "
                + "if value <= 0 then redis.call('del', KEYS[1]); return 0; end; "
                + "return value;";
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(key)));
        return result.longValue();
    }

    /**
     * 同时按账号和 IP 原子预占一次登录尝试。只有两个维度都未达到阈值时才增加计数，避免
     * 并发请求同时越过“先检查、后计数”的窗口。
     */
    public boolean reserveLoginAttempt(String accountKey,
                                       long accountLimit,
                                       String ipKey,
                                       long ipLimit,
                                       long expiredSeconds) {
        // 两个维度在一个脚本中先检查后递增，保证账号阈值和 IP 阈值面对并发时仍是硬上限。
        String script = "local account = tonumber(redis.call('get', KEYS[1]) or '0'); "
                + "local ip = tonumber(redis.call('get', KEYS[2]) or '0'); "
                + "if account >= tonumber(ARGV[1]) or ip >= tonumber(ARGV[2]) then return 0; end; "
                + "account = redis.call('incr', KEYS[1]); "
                + "ip = redis.call('incr', KEYS[2]); "
                + "if account == 1 then redis.call('expire', KEYS[1], ARGV[3]); end; "
                + "if ip == 1 then redis.call('expire', KEYS[2], ARGV[3]); end; "
                + "return 1;";
        Number result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(accountKey), fullKey(ipKey)),
                accountLimit, ipLimit, Math.max(1L, expiredSeconds));
        return result.longValue() == 1L;
    }

    /**
     * 登录成功后清除账号失败次数，并只归还本次预占的 IP 次数。IP 上其他账号产生的失败
     * 记录继续保留到统计窗口结束。
     */
    public void completeSuccessfulLoginAttempt(String accountKey, String ipKey) {
        // 账号认证成功后清除其历史失败状态；IP 维度只减去本请求预占的一次。
        String script = "redis.call('del', KEYS[1]); "
                + "if redis.call('exists', KEYS[2]) == 1 then "
                + "local value = redis.call('decr', KEYS[2]); "
                + "if value <= 0 then redis.call('del', KEYS[2]); end; end; "
                + "return 1;";
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE, script, RScript.ReturnType.INTEGER,
                List.of(fullKey(accountKey), fullKey(ipKey)));
    }

    private <T> RBucket<T> bucket(String key) {
        return redissonClient.getBucket(fullKey(key));
    }

    private RBucket<String> stringBucket(String key) {
        return redissonClient.getBucket(fullKey(key), StringCodec.INSTANCE);
    }

    private String fullKey(String key) {
        // 保留统一前缀扩展点，后续可按部署环境或应用增加命名空间而不改调用方。
        return REDIS_KEY_PREFIX + key;
    }

    private Duration duration(long value, TimeUnit timeUnit) {
        if (value <= 0) {
            // 非正数通常来自配置错误；使用有限默认值比创建永久缓存更安全。
            return Duration.ofSeconds(DEFAULT_EXPIRED_SECONDS);
        }
        return Duration.ofNanos(timeUnit.toNanos(value));
    }
}
