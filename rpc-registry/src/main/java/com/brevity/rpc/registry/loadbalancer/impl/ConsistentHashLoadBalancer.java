package com.brevity.rpc.registry.loadbalancer.impl;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcServiceHelper;
import com.brevity.rpc.common.ServiceMeta;
import com.brevity.rpc.common.exception.RpcRuntimeException;
import com.brevity.rpc.registry.loadbalancer.ServiceLoadBalancer;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一致性哈希负载均衡
 */
@Slf4j
public class ConsistentHashLoadBalancer implements ServiceLoadBalancer<ServiceMeta> {
    private static final int VIRTUAL_NODE_SIZE = 160;
    private final ReentrantLock lock = new ReentrantLock();

    private final ConcurrentHashMap<String, ConsistentHashSelector> SELECTTORS = new ConcurrentHashMap<>();

    @Override
    public ServiceMeta doSelect(List<ServiceMeta> servers, RpcRequest rpcRequest) {
        String serviceKey = RpcServiceHelper.buildServiceKey(rpcRequest.getClassName(), rpcRequest.getServiceVersion());
        int identityHashCode = servers.hashCode();
        ConsistentHashSelector selector = null;
        selector = SELECTTORS.get(serviceKey);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            try {
                lock.lock();
                selector = SELECTTORS.get(serviceKey);
                if (selector == null || selector.identityHashCode != identityHashCode) {
                    selector = new ConsistentHashSelector(servers, identityHashCode);
                    SELECTTORS.put(serviceKey, selector);
                }
            } finally {
                lock.unlock();
            }
        }
        ServiceMeta serviceMeta = selector.select(rpcRequest);
        return serviceMeta;
    }

    private class ConsistentHashSelector {
        private final TreeMap<Long, ServiceMeta> hashRing;
        private final int identityHashCode;

        private ConsistentHashSelector(List<ServiceMeta> servers, int identityHashCode) {
            this.identityHashCode = identityHashCode;
            this.hashRing = new TreeMap<>();

            for (ServiceMeta server : servers) {
                String s = server.toString();
                for (int i = 0; i < VIRTUAL_NODE_SIZE / 4; i++) {
                    byte[] digest = getMd5(s + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        hashRing.put(m, server);
                    }
                }
            }
        }

        public ServiceMeta select(RpcRequest rpcRequest) {
            byte[] digest = getMd5(rpcRequest.toString());
            Map.Entry<Long, ServiceMeta> entry = hashRing.ceilingEntry(hash(digest, 0));
            if (entry == null) {
                entry = hashRing.firstEntry();
            }
            return entry.getValue();
        }

        public byte[] getMd5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new RpcRuntimeException(e.getMessage(), e);
            }
            return md.digest();
        }

        public long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 |
                    (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }
    }
}
