package com.yxx.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.yxx.business", "com.yxx.common", "com.yxx.security", "com.yxx.framework",
        "com.yxx.rbac"})
@MapperScan({"com.yxx.business.mapper", "com.yxx.rbac.mapper"})
public class BusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessApplication.class, args);
    }

}
