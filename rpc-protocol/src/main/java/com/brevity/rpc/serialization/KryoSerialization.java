package com.brevity.rpc.serialization;

import com.brevity.rpc.common.RpcRequest;
import com.brevity.rpc.common.RpcResponse;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.sun.xml.ws.encoding.soap.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KryoSerialization implements RpcSerialization {

    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

    @Override
    public <T> byte[] serialize(T object) throws IOException {
        if (object == null) {
            throw new NullPointerException();
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output ot = new Output(byteArrayOutputStream);
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(ot, object);
            kryoThreadLocal.remove();
            return ot.toBytes();
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        if (data == null) {
            throw new NullPointerException("data can't be null");
        }
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            Input it = new Input(byteArrayInputStream);
            Kryo kryo = kryoThreadLocal.get();
            T result = kryo.readObject(it, clz);
            return result;
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }
}

























