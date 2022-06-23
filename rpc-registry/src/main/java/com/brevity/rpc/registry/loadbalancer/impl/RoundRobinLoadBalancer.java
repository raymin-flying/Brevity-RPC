package com.brevity.rpc.registry.loadbalancer.impl;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcServiceHelper;
import com.brevity.rpc.common.ServiceMeta;
import com.brevity.rpc.registry.loadbalancer.ServiceLoadBalancer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于权重的轮询算法
 */
public class RoundRobinLoadBalancer implements ServiceLoadBalancer<ServiceMeta> {

    private final ConcurrentHashMap<String, AtomicInteger> seqMap = new ConcurrentHashMap<>();

    @Override
    public ServiceMeta doSelect(List<ServiceMeta> servers, RpcRequest rpcRequest) {
        String serviceKey = RpcServiceHelper.buildServiceKey(rpcRequest.getClassName(), rpcRequest.getServiceVersion());
        int len = servers.size();
        int maxWeight = 0;
        int minWeight = Integer.MAX_VALUE;
        int[] weights = new int[len];
        int totalWeight = 0;
        for (int i = 0; i < len; i++) {
            ServiceMeta meta = servers.get(i);
            int weight = getWeight(meta);
            maxWeight = Math.max(weight, maxWeight);
            minWeight = Math.min(weight, minWeight);
            weights[i] = weight;
            totalWeight += weight;
        }
        AtomicInteger seq = seqMap.get(serviceKey);
        if (seq == null) {
            seqMap.putIfAbsent(serviceKey, new AtomicInteger(0));
            seq = seqMap.get(serviceKey);
        }
        int currentSeq = seq.getAndIncrement();
        if (minWeight > 0 && minWeight < maxWeight) {
            int mod = currentSeq % totalWeight;
            for (int i = 0; i < maxWeight; i++) {
                for (int j = 0; j < len; j++) {
                    if (mod == 0 && weights[j] > 0) {
                        return servers.get(j);
                    }
                    if (weights[j] > 0) {
                        weights[j]--;
                        mod--;
                    }
                }
            }
        }
        return servers.get(currentSeq % len);
    }

    private int getWeight(ServiceMeta serviceMeta) {
        int weight = serviceMeta.getWeight();
        if (weight > 0) {
            long createTime = serviceMeta.getCreateTime();
            long warmup = serviceMeta.getWarmup();
            long liveTime = System.currentTimeMillis() - createTime;
            if (liveTime > 0 && liveTime < warmup) {
                weight = calculateWarmupWeight(weight, liveTime, warmup);
            }
        }
        return weight;
    }

    private int calculateWarmupWeight(int weight, long liveTime, long warmup) {
        // (liveTime/warmup)*weight
        int afterWarmup = (int) ((float) liveTime / ((float) warmup / (float) weight));
        return afterWarmup < 1 ? 1 : afterWarmup;
    }
}
