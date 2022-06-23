package com.brevity.rpc.consumer;

import com.brevity.rpc.codec.RpcDecoder;
import com.brevity.rpc.codec.RpcEncoder;
import com.brevity.rpc.common.*;
import com.brevity.rpc.common.exception.RpcErrorMessageEnum;
import com.brevity.rpc.common.exception.RpcServiceException;
import com.brevity.rpc.protocol.RpcProtocol;
import com.brevity.rpc.registry.RegistryService;
import com.brevity.rpc.registry.loadbalancer.LoadBalancerFactory;
import com.brevity.rpc.registry.loadbalancer.LoadBalancerType;
import com.brevity.rpc.registry.loadbalancer.ServiceLoadBalancer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RpcConsumer {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final ChannelProvider channelProvider;
    private final ServiceLoadBalancer<ServiceMeta> loadBalancer;
    private final ReentrantLock lock = new ReentrantLock();

    public static final AttributeKey<Integer> LOSE_HEARTBEAT_COUNT = AttributeKey.valueOf("heartbeat_count");
    public final RpcConsumerConfig consumerConfig;

    /**
     * 初始化RPC客户端
     */
    public RpcConsumer(RpcConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
        channelProvider = ChannelProvider.getInstance();
        loadBalancer = LoadBalancerFactory.getInstance(LoadBalancerType.valueOf(consumerConfig.getLoadBalanceType()));
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(consumerConfig.getIoThreads());
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new IdleStateHandler(consumerConfig.getHEARTBEAT_TIME(),
                                        0, 0))
                                .addLast(new RpcEncoder())
                                .addLast(new RpcDecoder())
                                .addLast(new RpcReponseHandler(bootstrap, consumerConfig))
                        ;
                    }
                });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.warn("RpcConsumer stop!!!");
                eventLoopGroup.shutdownGracefully();
            }
        }));
    }

    /**
     * 服务发现->高可用->负载均衡
     */
    public void sendRequest(RpcProtocol<RpcRequest> protocol, RegistryService registryService) throws Exception {
        RpcRequest request = protocol.getBody();
        // 服务发现
        List<ServiceMeta> serviceMetaList = registryService.lookupService(request);
        // 高可用过滤
        List<ServiceMeta> hAServiceMetaList = channelProvider.chooseHAServiceMeta(serviceMetaList);
        // 负载均衡
        ServiceMeta serviceMeta = loadBalancer.select(hAServiceMetaList, protocol.getBody());
        if (serviceMeta != null) {
            Channel channel = getChannel(serviceMeta);
            channel.writeAndFlush(protocol).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("channel: " + channel.id().toString() + " send msg {id:" + protocol.getHeader().getRequestID() + "}");
                } else {
                    log.error("send msg {id:" + protocol.getHeader().getRequestID() + "} failed, cause: " + future.cause());
                    throw new RpcServiceException(RpcErrorMessageEnum.SERVICE_CALL_FAILURE);
                }
            });
        } else {
            log.error("no service find: " + request.getClassName());
            throw new RpcServiceException(RpcErrorMessageEnum.SERVICE_CAN_NOT_FOUND);
        }
    }

    /**
     * 复用channel
     */
    private Channel getChannel(ServiceMeta serviceMeta) throws RpcServiceException {
        Channel channel = channelProvider.get(serviceMeta);
        if (channel == null) {
            try {
                lock.lock();
                channel = channelProvider.get(serviceMeta);
                if (channel == null) {
                    ChannelFuture future = null;
                    try {
                        future = bootstrap.connect(serviceMeta.getServiceAddr(), serviceMeta.getServicePort()).sync();
                        log.info("connect rpc server {} on port {} success.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                        channel = future.channel();
                        channel.attr(RpcConsumer.LOSE_HEARTBEAT_COUNT).set(0);
                        channelProvider.put((InetSocketAddress) channel.remoteAddress(), channel);
                    } catch (Exception e) {
                        log.error("connect rpc server {} on port {} failed.", serviceMeta.getServiceAddr(), serviceMeta.getServicePort());
                        throw new RpcServiceException(RpcErrorMessageEnum.CLIENT_CONNECT_SERVER_FAILURE);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return channel;
    }
}