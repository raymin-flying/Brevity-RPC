package com.brevity.rpc.consumer;

import com.brevity.rpc.common.RpcConsumerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(RpcConsumerConfig.class)
public class RpcConsumerAutoConfiguration {

    @Resource
    private RpcConsumerConfig rpcConsumerConfig;

    @Bean
    public RpcConsumer rpcConsumer() {
        return new RpcConsumer(rpcConsumerConfig);
    }
}
