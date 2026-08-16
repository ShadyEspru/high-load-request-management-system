package com.hlrms.apigateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.resources.LoopResources;

@Configuration(proxyBeanMethods = false)
public class GatewayHttpClientEventLoopConfig {

    @Bean(
        name = "gatewayHttpClientLoopResources",
        destroyMethod = "dispose"
    )
    public LoopResources gatewayHttpClientLoopResources() {

        return LoopResources.create(
            "gateway-http-client",
            1,
            4,
            true
        );
    }

    @Bean
    public HttpClientCustomizer gatewayHttpClientCustomizer(
        @Qualifier("gatewayHttpClientLoopResources")
        LoopResources loopResources
    ) {

        return httpClient ->
            httpClient.runOn(
                loopResources,
                true
            );
    }
}
