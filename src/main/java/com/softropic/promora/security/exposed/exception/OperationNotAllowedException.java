package com.softropic.promora.security.exposed.exception;



import com.softropic.promora.common.exception.ErrorCode;

import java.util.Map;

public class OperationNotAllowedException extends AuthorizationException {
    public OperationNotAllowedException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }

    public OperationNotAllowedException(String msg, Map<String, Object> logContext, ErrorCode errorCode) {
        super(msg, logContext, errorCode);
    }
}
