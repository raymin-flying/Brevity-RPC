package com.brevity.rpc.common;

public class RpcServiceHelper {
    public static String buildServiceKey(String serviceName, String serviceVersion) {
        return "".join("#", serviceName, serviceVersion);
    }

    public static String buildRegistryPath(ServiceMeta serviceMeta) {
        return "/" + serviceMeta.getServiceName() + "#" + serviceMeta.getServiceVersion()
                + "/" + serviceMeta.getServiceAddr() + ":" + serviceMeta.getServicePort();
    }

}
