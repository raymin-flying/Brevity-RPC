package com.brevity.rpc.common;

import lombok.Data;

/**
 * 描述服务的元数据类，用来服务注册，服务发现
 */
@Data
public class ServiceMeta {

    private String serviceName;

    private String serviceVersion;

    private String serviceAddr;

    private int servicePort;

    // 以下是实现权重负载均衡的参数
    private int weight; // 权重为0-100，默认值为50

    private long createTime; // 用System.currentTimeMillis()生成

    private long warmup; // 默认为5分钟

}
