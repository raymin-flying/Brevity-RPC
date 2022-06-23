package com.brevity.rpc.registry.loadbalancer;

import com.brevity.rpc.registry.loadbalancer.impl.ConsistentHashLoadBalancer;
import com.brevity.rpc.registry.loadbalancer.impl.RoundRobinLoadBalancer;
import com.brevity.rpc.registry.loadbalancer.impl.RandomLoadBalancer;

public class LoadBalancerFactory {

    private static volatile ServiceLoadBalancer loadBalancer;

    public static ServiceLoadBalancer getInstance(LoadBalancerType type) {
        if (null == loadBalancer) {
            synchronized (LoadBalancerFactory.class) {
                if (null == loadBalancer) {
                    switch (type) {
                        case Random:
                            loadBalancer = new RandomLoadBalancer();
                            break;
                        case RoundRobin:
                            loadBalancer=new RoundRobinLoadBalancer();
                            break;
                        default: // 默认采用一致性哈希
                            loadBalancer = new ConsistentHashLoadBalancer();
                            break;
                    }
                }
            }
        }
        return loadBalancer;
    }
}
