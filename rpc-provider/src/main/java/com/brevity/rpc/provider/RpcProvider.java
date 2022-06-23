package com.brevity.rpc.provider;

import com.brevity.rpc.codec.RpcDecoder;
import com.brevity.rpc.codec.RpcEncoder;
import com.brevity.rpc.common.RpcServiceConfig;
import com.brevity.rpc.common.RpcServiceHelper;
import com.brevity.rpc.common.ServiceMeta;
import com.brevity.rpc.handler.RpcRequestHandler;
import com.brevity.rpc.handler.RpcRequestProcessor;
import com.brevity.rpc.provider.annotation.RpcService;
import com.brevity.rpc.registry.RegistryFactory;
import com.brevity.rpc.registry.RegistryService;
import com.brevity.rpc.registry.RegistryType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RpcProvider implements InitializingBean, BeanPostProcessor {

    private final RpcServiceConfig serviceConfig;
    private String serviceAddress;
    private final RegistryService registryService;
    private final Map<String, Object> rpcServiceMap = new HashMap<>();

    public RpcProvider(RpcServiceConfig serviceConfig) throws UnknownHostException {
        this.serviceConfig = serviceConfig;
        serviceAddress = InetAddress.getLocalHost().getHostAddress();
        RegistryType registryType = RegistryType.valueOf(serviceConfig.getRegistryType());
        registryService = RegistryFactory.getInstance(serviceConfig.getRegistryAddr(), registryType);
    }


    @Override
    public void afterPropertiesSet() {
        new Thread(() -> {
            try {
                startServer();
            } catch (Exception e) {
                log.error("start brevity-rpc server error.", e);
            }
        }).start();
    }

    private void startServer() throws Exception {
        NioEventLoopGroup boss = new NioEventLoopGroup(serviceConfig.getBossThreads());
        NioEventLoopGroup work = new NioEventLoopGroup(serviceConfig.getWorkThreads());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, work).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new IdleStateHandler(0, 0,
                                            serviceConfig.getAllIdleTimeSeconds()))
                                    .addLast(new RpcEncoder())
                                    .addLast(new RpcDecoder())
                                    .addLast(new RpcRequestHandler(rpcServiceMap))
                            ;
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);
            RpcRequestProcessor.init(serviceConfig.getHandleCoreThreads(), serviceConfig.getHandleMaxThreads(),
                    serviceConfig.getHandleQueueSize());

            ChannelFuture channelFuture = bootstrap.bind(this.serviceAddress, serviceConfig.getServicePort()).sync();
            log.info("Rpc Server addr {} started on port {}", this.serviceAddress, serviceConfig.getServicePort());
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    log.warn("Server stop!!!");
                    // 关闭线程池
                    RpcRequestProcessor.close();
                }
            }));
            channelFuture.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
        if (rpcService != null) {
            String serviceName = rpcService.serviceInterface().getName();
            String serviceVersion = rpcService.serviceVersion();
            try {
                // 准备发布服务
                ServiceMeta serviceMeta = new ServiceMeta();
                serviceMeta.setServiceAddr(serviceAddress);
                serviceMeta.setServicePort(serviceConfig.getServicePort());
                serviceMeta.setServiceName(serviceName);
                serviceMeta.setServiceVersion(serviceVersion);
                serviceMeta.setCreateTime(System.currentTimeMillis());
                serviceMeta.setWeight(rpcService.weight());
                serviceMeta.setWarmup(rpcService.warmup());
                registryService.register(serviceMeta);

                rpcServiceMap.put(RpcServiceHelper.buildServiceKey(serviceName, serviceVersion), bean);
            } catch (Exception e) {
                log.error("failed to register service {}#{}", serviceName, serviceVersion, e);
            }
        }
        return bean;
    }
}