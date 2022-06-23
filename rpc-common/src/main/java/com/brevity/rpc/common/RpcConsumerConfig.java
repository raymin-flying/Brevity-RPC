package com.brevity.rpc.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rpc.consumer")
public class RpcConsumerConfig {

    private int ioThreads = 2; // netty客户端IO线程数

    private String registryType = "ZOOKEEPER"; // 注册中心类型

    private String registryAddress = "127.0.0.1:2181"; // 注册中心地址

    private String loadBalanceType = "ConsistentHash"; // 负载均衡策略

    private int HEARTBEAT_TIME = 8; // 单位秒,该空闲时间内无读事件，触发心跳

    private int MAX_LOSE_HEARTBEAT_COUNT = 5; // 丢失心跳最大次数后，关闭连接

    private int MAX_RETRY_COUNT = 3; // 异常断开最大重连次数

    private int RETRY_TIME = 10; // 每次重试的间隔

    private String SerializationType = "Kryo"; // 序列化方式

}
