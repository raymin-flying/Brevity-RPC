package com.brevity.rpc.provider.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RpcService {

    Class<?> serviceInterface() default Object.class;

    String serviceVersion() default "1.0.0";

    int weight() default 50; // 权重0-100，默认为50

    long warmup() default 300000; // 默认为5分钟

}
