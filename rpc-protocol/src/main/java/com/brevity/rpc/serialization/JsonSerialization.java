package com.brevity.rpc.serialization;

import com.alibaba.fastjson.JSON;

import java.io.IOException;

/**
 * 使用fastjson实现json格式的序列化和反序列化
 * 注意：序列化的对象需要实现getter和setting方法
 */
public class JsonSerialization implements RpcSerialization {
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        if (object == null) {
            throw new NullPointerException("序列化对象不能为null");
        }
        byte[] data = JSON.toJSONBytes(object);
        return data;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        T o = JSON.parseObject(data, clz);
        return o;
    }
}
