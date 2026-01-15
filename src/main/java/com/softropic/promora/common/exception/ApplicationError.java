package com.softropic.promora.common.exception;

public enum ApplicationError implements ErrorCode {
    ENTITY_NOT_FOUND,
    UNKNOWN, //When failure occurs while trying to update
    ;

    @Override
    public String getErrorCode() {
        return this.name();
    }
}
