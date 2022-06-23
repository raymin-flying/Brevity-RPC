package com.brevity.rpc.consumer;

import com.brevity.rpc.consumer.annotation.RpcReference;
import com.brevity.rpc.registry.RegistryFactory;
import com.brevity.rpc.registry.RegistryService;
import com.brevity.rpc.registry.RegistryType;
import com.brevity.rpc.serialization.SerializationTypeEnum;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * 对带有RpcReference注解的bean生成代理对象
 */
@Component
public class ConsumerPostProcessor implements BeanPostProcessor {

    @Autowired
    private RpcConsumer rpcConsumer;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RegistryService registryService = RegistryFactory.getInstance(
                        rpcConsumer.consumerConfig.getRegistryAddress(),
                        RegistryType.valueOf(rpcConsumer.consumerConfig.getRegistryType()));
                Object proxyInstance = Proxy.newProxyInstance(
                        rpcReference.getClass().getClassLoader(),
                        new Class<?>[]{declaredField.getType()},
                        new RpcInvokerProxy(rpcReference.serviceVersion(), rpcReference.timeout(),
                                registryService, rpcConsumer,
                                SerializationTypeEnum.valueOf(rpcConsumer.consumerConfig.getSerializationType()))
                );
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, proxyInstance);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
        return bean;
    }
}
