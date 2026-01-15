package com.softropic.promora.security.exposed.exception;


import com.softropic.promora.common.exception.ApplicationException;
import com.softropic.promora.common.exception.ErrorCode;

public class EncryptionException extends ApplicationException {
    public EncryptionException(String msg,
                               ErrorCode errorCode) {
        super(msg, errorCode);
    }

    public EncryptionException(String msg, Throwable cause, ErrorCode errorCode) {
        super(msg, cause, errorCode);
    }
}
