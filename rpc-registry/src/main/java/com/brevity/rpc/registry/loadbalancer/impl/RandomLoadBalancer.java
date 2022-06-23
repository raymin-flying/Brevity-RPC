package com.brevity.rpc.registry.loadbalancer.impl;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.ServiceMeta;
import com.brevity.rpc.registry.loadbalancer.ServiceLoadBalancer;

import java.util.List;
import java.util.Random;

/**
 * 基于权重的随机负载均衡策略，如果权重相等，那么就是随机选一个节点，如果不相等，则权重越大的越容易选中
 */
public class RandomLoadBalancer implements ServiceLoadBalancer<ServiceMeta> {

    private final Random random = new Random();

    @Override
    public ServiceMeta doSelect(List<ServiceMeta> servers, RpcRequest rpcRequest) {
        int[] weights = new int[servers.size()];
        weights[0] = getWeight(servers.get(0));
        int pre_weight = weights[0];
        boolean same_weight = true;
        int total_weights = weights[0];
        for (int i = 1; i < servers.size(); i++) {
            weights[i] = getWeight(servers.get(i));
            total_weights += weights[i];
            if (same_weight && pre_weight != weights[i]) {
                same_weight = false;
            }
            pre_weight = weights[i];
        }
        if (total_weights > 0 && !same_weight) {
            int offset = random.nextInt(total_weights);
            for (int i = 0; i < weights.length; i++) {
                offset -= weights[i];
                if (offset < 0) {
                    return servers.get(i);
                }
            }
        }
        return servers.get(random.nextInt(weights.length));
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
