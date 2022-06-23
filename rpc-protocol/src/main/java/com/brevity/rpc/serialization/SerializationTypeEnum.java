package com.brevity.rpc.serialization;

import lombok.Getter;

public enum SerializationTypeEnum {
    HESSIAN(0x10),
    JSON(0x20),
    Kryo(0x30);

    @Getter
    private final int type;

    SerializationTypeEnum(int type) {
        this.type = type;
    }

    public static SerializationTypeEnum findByType(byte serializationType) {
        for (SerializationTypeEnum typeEnum : SerializationTypeEnum.values()) {
            if (typeEnum.getType() == serializationType) {
                return typeEnum;
            }
        }
        return HESSIAN; // 如果没找到，则默认使用HESSIAN
    }
}
