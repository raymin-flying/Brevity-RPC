package com.brevity.rpc.serialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializationFactory {

    private static Map<Byte, RpcSerialization> cache = new ConcurrentHashMap<>(4);

    public static RpcSerialization getRpcSerialization(byte serializationType) {
        RpcSerialization instance = cache.get(serializationType);
        if (instance == null) {
            synchronized (SerializationFactory.class) {
                instance = cache.get(serializationType);
                if (instance == null) {
                    SerializationTypeEnum type = SerializationTypeEnum.findByType(serializationType);
                    instance = getRpcSerialization(type);
                    cache.put(serializationType, instance);
                }
            }
        }
        return instance;
    }

    private static RpcSerialization getRpcSerialization(SerializationTypeEnum type) {
        switch (type) {
            case JSON:
                return new JsonSerialization();
            case HESSIAN:
                return new HessianSerialization();
            case Kryo:
                return new KryoSerialization();
            default:
                throw new IllegalArgumentException("serialization type is illegal, " + type);
        }
    }
}
