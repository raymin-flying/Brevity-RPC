package com.brevity.rpc.registry;

public class RegistryFactory {
    private static volatile RegistryService registryService;

    public static RegistryService getInstance(String registryAddr, RegistryType registryType) {
        if (null == registryService) {
            synchronized (RegistryFactory.class) {
                if (null == registryService) {
                    switch (registryType) {
                        case EUREKA:
                            registryService = new EurekaRegistryService(registryAddr);
                            break;
                        case ZOOKEEPER:
                            registryService = new ZookeeperRegistryService(registryAddr);
                            break;
                        default:
                            registryService = new ZookeeperRegistryService(registryAddr);
                            break;
                    }
                }
            }
        }
        return registryService;
    }
}
