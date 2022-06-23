package com.brevity.rpc.serialization;

import java.io.IOException;

/**
 * 所有的序列化方法都需实现这个接口来进行序列化
 */
public interface RpcSerialization {

    <T> byte[] serialize(T obj) throws IOException;

    <T> T deserialize(byte[] data, Class<T> clz) throws IOException;

}