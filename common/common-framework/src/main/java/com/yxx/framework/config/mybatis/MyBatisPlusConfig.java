package com.yxx.framework.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yxx.framework.hander.CommonMetaObjectHandler;
import com.yxx.framework.interceptor.mybatis.ParameterInterceptor;
import com.yxx.framework.interceptor.mybatis.ResultSetInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yxx
 */
@Configuration
public class MyBatisPlusConfig {
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 自动填充参数
     */
    @Bean
    public CommonMetaObjectHandler commonMetaObjectHandler() {
        return new CommonMetaObjectHandler();
    }

    /**
     * 注解加密
     */
    @Bean
    public ParameterInterceptor encrypt() {
        return new ParameterInterceptor();
    }

    /**
     * 注解解密
     */
    @Bean
    public ResultSetInterceptor decrypt() {
        return new ResultSetInterceptor();
    }


}