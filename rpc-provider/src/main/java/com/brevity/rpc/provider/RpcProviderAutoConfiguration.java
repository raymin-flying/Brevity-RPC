package com.brevity.rpc.provider;

import com.brevity.rpc.common.RpcServiceConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties({RpcServiceConfig.class})
public class RpcProviderAutoConfiguration {

    @Resource
    private RpcServiceConfig rpcProperties;

    @Bean
    public RpcProvider rpcProvider() throws Exception {
        return new RpcProvider(rpcProperties);
    }
}
