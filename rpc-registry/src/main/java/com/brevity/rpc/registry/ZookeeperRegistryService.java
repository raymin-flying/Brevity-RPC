package com.brevity.rpc.registry;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcServiceHelper;
import com.brevity.rpc.common.ServiceMeta;
import com.brevity.rpc.common.exception.RpcErrorMessageEnum;
import com.brevity.rpc.common.exception.RpcServiceException;
import com.brevity.rpc.serialization.RpcSerialization;
import com.brevity.rpc.serialization.SerializationFactory;
import com.brevity.rpc.serialization.SerializationTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public final class ZookeeperRegistryService implements RegistryService {
    public static final int SLEEP_TIME_MS = 1000;
    public static final int MAX_RETRIES = 3;
    public static final String NAMESPACE = "brevity_rpc";
    public static CuratorFramework curator;

    private final Map<String, List<ServiceMeta>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private final Set<String> SERVICE_PUBLISH_SET = ConcurrentHashMap.newKeySet();
    private final RpcSerialization serialization = SerializationFactory.getRpcSerialization((byte) SerializationTypeEnum.JSON.getType());
    private final ReentrantLock lock = new ReentrantLock();

    public ZookeeperRegistryService(String registryAddr) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(SLEEP_TIME_MS, MAX_RETRIES);
        curator = CuratorFrameworkFactory.builder().connectString(registryAddr)
                .retryPolicy(retryPolicy)
                .namespace(NAMESPACE)
                .build();
        curator.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("close session: " + curator.toString());
            curator.close();
        }));
    }

    @Override
    public void register(ServiceMeta serviceMeta) throws Exception {
        String path = RpcServiceHelper.buildRegistryPath(serviceMeta);
        try {
            if (SERVICE_PUBLISH_SET.contains(path) && curator.checkExists().forPath(path) != null) {
                log.info("The service already register. path: " + path);
            } else {
                byte[] data = serialization.serialize(serviceMeta);
                curator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                        .forPath(path, data);
                log.info("Register service: " + path + " successfully");
                SERVICE_PUBLISH_SET.add(path);
            }
        } catch (Exception e) {
            throw new RpcServiceException(RpcErrorMessageEnum.REGISTER_SERVICE_FAIL);
        }
    }

    @Override
    public void unregister(ServiceMeta serviceMeta) throws Exception {

    }

    /**
     * 服务发现
     */
    @Override
    public List<ServiceMeta> lookupService(RpcRequest rpcRequest) throws RpcServiceException {
        List<ServiceMeta> serviceMetaList = null;
        String serviceKey = RpcServiceHelper.buildServiceKey(rpcRequest.getClassName(), rpcRequest.getServiceVersion());
        if (SERVICE_ADDRESS_MAP.containsKey(serviceKey)) {
            serviceMetaList = SERVICE_ADDRESS_MAP.get(serviceKey);
        } else {
            try {
                lock.lock();
                serviceMetaList = SERVICE_ADDRESS_MAP.get(serviceKey);
                if (serviceMetaList == null) {
                    String servicePath = "/" + serviceKey;
                    List<String> childs = curator.getChildren().forPath(servicePath);
                    List<String> result = new ArrayList<>();
                    for (String child : childs) {
                        byte[] bytes = curator.getData().forPath(servicePath + "/" + child);
                        result.add(new String(bytes, StandardCharsets.UTF_8));
                    }
                    serviceMetaList = getServiceFromZK(serviceKey);
                    SERVICE_ADDRESS_MAP.put(serviceKey, serviceMetaList);
                    notifyListener(serviceKey);
                }
            } catch (Exception e) {
                log.error("Registery error：", e.getMessage());
                throw new RpcServiceException(RpcErrorMessageEnum.REGISTERY_SERVER_ERROR, e.getMessage());
            } finally {
                lock.unlock();
            }
        }
        return serviceMetaList;
    }

    public List<ServiceMeta> getServiceFromZK(String serviceKey) throws Exception {
        String servicePath = "/" + serviceKey;
        List<String> childs = curator.getChildren().forPath(servicePath);
        List<ServiceMeta> serviceMetaList = new ArrayList<>(childs.size());
        for (String child : childs) {
            byte[] bytes = curator.getData().forPath(servicePath + "/" + child);
            serviceMetaList.add(serialization.deserialize(bytes, ServiceMeta.class));
        }
        return serviceMetaList;
    }

    @Override
    public void notifyListener(String serviceKey) throws Exception {
        String servicePath = "/" + serviceKey;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(curator, servicePath, true);
        PathChildrenCacheListener listener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                List<ServiceMeta> serviceMetaList = getServiceFromZK(serviceKey);
                log.warn("ServiceMetaList changed: " + serviceKey);
                SERVICE_ADDRESS_MAP.put(serviceKey, serviceMetaList);
            }
        };
        pathChildrenCache.getListenable().addListener(listener);
        pathChildrenCache.start();
    }

    @Override
    public void destory() throws Exception {

    }
}
