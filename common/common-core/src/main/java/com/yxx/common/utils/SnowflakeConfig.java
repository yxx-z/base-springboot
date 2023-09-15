package com.yxx.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yxx
 */
@Component
@Slf4j
public class SnowflakeConfig {
    /**
     * 终端ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private long workerId;

    /**
     * 数据中心ID
     */
    private final long datacenterId = 1;
    private final Snowflake snowflake = IdUtil.getSnowflake(workerId, datacenterId);

    @PostConstruct
    public void init() {
        workerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr());
        log.info("当前机器的workId:{}", workerId);
    }

    public String orderNum() {
        // 获取当前日期，格式化为yyyyMMdd
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new Date());
        return currentDate + snowflakeId();
    }

    public synchronized long snowflakeId() {
        return snowflake.nextId();
    }

    public synchronized long snowflakeId(long workerId, long datacenterId) {
        Snowflake snowflake = IdUtil.getSnowflake(workerId, datacenterId);
        return snowflake.nextId();
    }
}

