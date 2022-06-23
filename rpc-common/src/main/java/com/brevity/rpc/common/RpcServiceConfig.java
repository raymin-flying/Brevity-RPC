package com.brevity.rpc.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rpc.service")
public class RpcServiceConfig {

    private int servicePort = 2526; // 发布服务的端口

    private String registryAddr = "127.0.0.1:2181"; // 注册中心的ip:port

    private String registryType = "ZOOKEEPER"; // 注册中心类型

    private int bossThreads = 2; // netty服务端reactor线程数

    private int workThreads = 10; // netty服务端work线程数

    private int handleCoreThreads = 100; // 业务线程池核心线程数

    private int handleMaxThreads = 200; // 业务线程池最大线程数

    private int handleQueueSize = 500; // 业务线程池等待队列最大数

    private int allIdleTimeSeconds = 30; // 超过该空闲时间无读写事件发生，则关闭连接

}

