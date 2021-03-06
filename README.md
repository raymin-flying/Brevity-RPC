# Brevity-RPC

一款基于Netty+Kryo+ZooKeeper+Spring Boot实现的轻量级RPC框架。

### 特征

- 基于Netty的主从Reactor模型进行网络通信
- 默认使用ZooKeeper作为注册中心，自动服务注册和服务发现，支持拓展其他注册中心
- 默认使用Kryo序列化算法，并实现了Json、Hessian序列化算法，支持拓展
- 支持多种负载均衡策略，实现了一致性哈希负载均衡，基于权重的随机、轮询负载均衡算法，并支持拓展
- 服务高可用，支持服务列表自动更新，异常服务节点自动剔除
- 支持心跳机制，异常断开重连，保持客户端与服务端的长连接
- 基于Spring自定义注解，轻松实现服务发布和调用，支持对不同机器上的服务分配权重
- 自定义传输协议，减少不必要的字段传输
- 统一的异常管理

### 整体架构

![framework](./images/framework.png)

### 协议

![image-20220623212739004](./images/protocal.png)

### 配置信息

#### RPC客户端

```java
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
```

#### RPC服务端

```java
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
```

### 运行项目

#### 注册中心

推荐使用ZooKeeper 3.5.8版本，curator 4.2.0搭配使用

在客户端和服务端指定注册中心地址，服务即会被注册到ZooKeeper服务端 /brevity-rpc/服务名的路径下

```java
rpc.consumer.registryAddress=127.0.0.1:2181
rpc.provider.registryAddress=127.0.0.1:2181
```

#### RPC服务端

在rpc-facade模块中定义需要发布的服务接口，并在服务端使用@RpcService实现该接口，启动服务端即可发布服务。

@RpcService注解需要四个参数：服务的接口类型，服务版本号，权重和预热时间。其中，权重默认为50，可指定0-100范围；预热时间默认为五分钟。权重和预热时间可用于基于权重的随机和轮询负载均衡算法。

- 定义服务接口

```java
public interface HelloFacade {
    
    String helloRpc(String name);

    String helloRpc(String name,String address);
}
```

- 实现服务接口

```java
@RpcService(serviceInterface = HelloFacade.class, serviceVersion = "1.0.0")
public class HelloFacadeImpl implements HelloFacade {

    @Override
    public String helloRpc(String name) {
        return "HelloFacade: " + name;
    }

    @Override
    public String helloRpc(String name, String address) {
        return "HelloFacade: hello " + name + ", my friend from " + address;
    }
}
```

#### RPC客户端

在需要调用RPC服务的对象上使用@RpcReference注解，并指定服务版本号，即可实现透明调用RPC服务。

```java
@Component
public class MyController {
    @RpcReference(serviceVersion = "1.0.0")
    private HelloFacade helloFacade;

    @RpcReference(serviceVersion = "1.0.0")
    private ArrayFacade arrayFacade;

    public void test() {
        for(int i=0;i<10;i++){
            System.out.println(helloFacade.helloRpc("xiaoming"));
            System.out.println(helloFacade.helloRpc("xiaoming","guangzhou"));
            System.out.println(arrayFacade.hello("brevity"));
        }
    }
}
```

