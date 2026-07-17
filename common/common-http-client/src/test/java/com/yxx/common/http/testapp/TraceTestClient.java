package com.yxx.common.http.testapp;

import com.dtflys.forest.annotation.ForestClient;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Var;

/** 仅用于验证公共 Forest 自动配置的测试客户端。 */
@ForestClient
public interface TraceTestClient {

    /**
     * 调用本地测试服务。
     *
     * @param baseUrl MockWebServer 基础地址
     * @return 响应文本
     */
    @Get("${baseUrl}/trace")
    String getTrace(@Var("baseUrl") String baseUrl);
}
