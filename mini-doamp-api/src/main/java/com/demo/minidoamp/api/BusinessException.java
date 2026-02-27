package com.demo.minidoamp.api;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }
}
