package com.softropic.promora.security.exception;

import com.softropic.promora.common.exception.ErrorCode;
import com.softropic.promora.security.exposed.exception.AuthorizationException;

import java.util.Map;

public class ProfileActionException extends AuthorizationException {

    public ProfileActionException(String msg, ErrorCode errorCode) {
        super(msg, errorCode);
    }

    public ProfileActionException(String msg, Throwable cause, ErrorCode errorCode) {
        super(msg, cause, errorCode);
    }

    public ProfileActionException(String msg,
                                  Throwable cause,
                                  Map<String, Object> logContext,
                                  ErrorCode errorCode) {
        super(msg, cause, logContext, errorCode);
    }

    public ProfileActionException(String msg, Map<String, Object> logContext, ErrorCode errorCode) {
        super(msg, logContext, errorCode);
    }
}
