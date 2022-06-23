package com.brevity.rpc.common;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 对channel进行管理，单例模式
 */
@Slf4j
public class ChannelProvider {

    private final Map<String, Node> channelMap;

    private static ChannelProvider instance;

    public static ChannelProvider getInstance() {
        if (instance == null) {
            synchronized (ChannelProvider.class) {
                if (instance == null) {
                    instance = new ChannelProvider();
                }
            }
        }
        return instance;
    }

    @Data
    public static class Node {
        Channel channel;
        boolean retry;
        int retry_count = 0;

        public Node(Channel channel, boolean retry) {
            this.channel = channel;
            this.retry = retry;
        }
    }

    private ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(ServiceMeta serviceMeta) {
        String address = serviceMeta.getServiceAddr() + serviceMeta.getServicePort();
        if (channelMap.containsKey(address)) {
            Channel channel = channelMap.get(address).channel;
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(address);
                if (channel != null) channel.close();
            }
        }
        return null;
    }

    public Node get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.getHostString() + inetSocketAddress.getPort();
        return channelMap.get(key);
    }

    public void put(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.getHostString() + inetSocketAddress.getPort();
        channelMap.put(key, new Node(channel, false));
        log.info("ChannelMap insert new channel: " + channel.toString());
    }

    public void setChannelRetry(InetSocketAddress inetSocketAddress, boolean flag) {
        String key = inetSocketAddress.getHostString() + inetSocketAddress.getPort();
        Node node = channelMap.get(key);
        node.retry = flag;
        channelMap.put(key, node);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.getHostString() + inetSocketAddress.getPort();
        log.info("ChannelMap remove channel: " + key);
        channelMap.remove(key);
    }

    public List<ServiceMeta> chooseHAServiceMeta(List<ServiceMeta> serviceMetaList) {
        List<ServiceMeta> hAServiceMetaList = null;
        hAServiceMetaList = serviceMetaList.stream().filter(new Predicate<ServiceMeta>() {
                    @Override
                    public boolean test(ServiceMeta serviceMeta) {
                        String key = serviceMeta.getServiceAddr() + serviceMeta.getServicePort();
                        return !channelMap.containsKey(key) || !channelMap.get(key).retry;
                    }
                })
                .collect(Collectors.toList());
        return hAServiceMetaList;
    }
}