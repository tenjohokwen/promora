package com.softropic.promora.security.exposed.exception;


import com.softropic.promora.common.exception.ErrorCode;

public enum EncryptionError implements ErrorCode {
    MISSING_SECRET,
    MISSING_TEXT,
    ENCRYPTION_ERROR,
    DECRYPTION_ERROR;

    @Override
    public String getErrorCode() {
        return this.name();
    }
}
