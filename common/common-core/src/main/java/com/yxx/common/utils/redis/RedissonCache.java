package com.yxx.common.utils.redis;

import com.yxx.common.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
 
/**
 * @author yxx
 * @description redisson工具类
 */
@Slf4j
@Configuration
public class RedissonCache {
 
    /**
     * 默认缓存时间
     */
    private static final Long DEFAULT_EXPIRED = 5 * 60L;
    /**
     * redis key前缀
     */
    private static final String REDIS_KEY_PREFIX = "";
    /**
     * redisson client对象
     */
    private RedissonClient redisson;

    @Resource
    private RedisProperties redisProperties;

    /**
     * 初始化连接
     *
     * @throws IOException
     */
    @PostConstruct
    public void init(){
        // 初始化配置
        Config config = new Config();
        // redis url
        String redisUrl = String.format("redis://%s:%s", redisProperties.getHost() + Constant.EMPTY, redisProperties.getPort() + Constant.EMPTY);
        // 设置url和密码
        config.useSingleServer().setAddress(redisUrl).setPassword(redisProperties.getPassword());
        // 设置库
        config.useSingleServer().setDatabase(redisProperties.getDatabase());
        // 设置最小空闲Redis连接量
        config.useSingleServer().setConnectionMinimumIdleSize(10);
        // 设置Redis数据编解码器 序列化方式
        Codec codec = new JsonJacksonCodec();
        config.setCodec(codec);
        if (redisson == null) {
            redisson = Redisson.create(config);
            log.info( "redis连接成功,server={}", redisUrl);
        } else {
            log.warn("redis 重复连接,config={}", config);
        }
    }
 
    /**
     * 读取缓存
     *
     * @param key 缓存key
     * @param <T>
     * @return 缓存返回值
     */
    public <T> T get(String key) {
        RBucket<T> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key);
        return bucket.get();
    }

    /**
     * 判断key是否存在
     *
     * @param key key
     * @return {@link Boolean }
     * @author yxx
     */
    public Boolean isExists(String key) {
        return redisson.getBucket(REDIS_KEY_PREFIX + key).isExists();
    }
 
    /**
     * 以string的方式读取缓存
     *
     * @param key 缓存key
     * @return 缓存返回值
     */
    public String getString(String key) {
        RBucket<String> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key, StringCodec.INSTANCE);
        return bucket.get();
    }
 
    /**
     * 设置缓存（注：redisson会自动选择序列化反序列化方式）
     *
     * @param key   缓存key
     * @param value 缓存值
     * @param <T>
     */
    public <T> void put(String key, T value) {
        RBucket<T> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key);
        bucket.set(value, DEFAULT_EXPIRED, TimeUnit.SECONDS);
    }
 
    /**
     * 以string的方式设置缓存
     *
     * @param key
     * @param value
     */
    public void putString(String key, String value) {
        RBucket<String> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key, StringCodec.INSTANCE);
        bucket.set(value, DEFAULT_EXPIRED, TimeUnit.SECONDS);
    }
 
    /**
     * 以string的方式保存缓存（与其他应用共用redis时需要使用该函数）
     *
     * @param key     缓存key
     * @param value   缓存值
     * @param expired 缓存过期时间
     */
    public void putString(String key, String value, long expired) {
        RBucket<String> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key, StringCodec.INSTANCE);
        bucket.set(value, expired <= 0 ? DEFAULT_EXPIRED : expired, TimeUnit.SECONDS);
    }
 
    /**
     * 如果不存在则写入缓存（string方式，不带有redisson的格式信息）
     *
     * @param key     缓存key
     * @param value   缓存值
     * @param expired 缓存过期时间
     */
    public boolean putStringIfAbsent(String key, String value, long expired) {
        RBucket<String> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key, StringCodec.INSTANCE);
        return bucket.trySet(value, expired <= 0 ? DEFAULT_EXPIRED : expired, TimeUnit.SECONDS);
    }
 
    /**
     * 如果不存在则写入缓存（string方式，不带有redisson的格式信息）（不带过期时间，永久保存）
     *
     * @param key     缓存key
     * @param value   缓存值
     */
    public boolean putStringIfAbsent(String key, String value) {
        RBucket<String> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key, StringCodec.INSTANCE);
        return bucket.trySet(value);
    }
 
    /**
     * 设置缓存
     *
     * @param key     缓存key
     * @param value   缓存值
     * @param expired 缓存过期时间
     * @param <T>     类型
     */
    public <T> void put(String key, T value, long expired) {
        RBucket<T> bucket = redisson.getBucket(REDIS_KEY_PREFIX + key);
        bucket.set(value, expired <= 0 ? DEFAULT_EXPIRED : expired, TimeUnit.SECONDS);
    }
 
    /**
     * 移除缓存
     *
     * @param key
     */
    public void remove(String key) {
        redisson.getBucket(REDIS_KEY_PREFIX + key).delete();
    }
 
    /**
     * 判断缓存是否存在
     *
     * @param key
     * @return
     */
    public boolean exists(String key) {
        return redisson.getBucket(REDIS_KEY_PREFIX + key).isExists();
    }
 
 
    /**
     * 暴露redisson的RList对象
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> RList<T> getRedisList(String key) {
        return redisson.getList(REDIS_KEY_PREFIX + key);
    }
 
    /**
     * 暴露redisson的RMapCache对象
     *
     * @param key
     * @param <K>
     * @param <V>
     * @return
     */
    public <K, V> RMapCache<K, V> getRedisMap(String key) {
        return redisson.getMapCache(REDIS_KEY_PREFIX + key);
    }
 
    /**
     * 暴露redisson的RSET对象
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> RSet<T> getRedisSet(String key) {
        return redisson.getSet(REDIS_KEY_PREFIX + key);
    }
 
 
    /**
     * 暴露redisson的RScoredSortedSet对象
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> RScoredSortedSet<T> getRedisScoredSortedSet(String key) {
        return redisson.getScoredSortedSet(REDIS_KEY_PREFIX + key);
    }
 
    /**
     * 获取redisson的Lock对象
     *
     * @param key key
     * @return 锁
     */
    public RLock getRedisLock(String key) {
        return redisson.getLock(REDIS_KEY_PREFIX + key);
    }
 
 
    @PreDestroy
    public void close() {
        try {
            if (redisson != null) {
                redisson.shutdown();
            }
        } catch (Exception ex) {
            log.error( ex.getMessage(), ex);
        }
    }

}