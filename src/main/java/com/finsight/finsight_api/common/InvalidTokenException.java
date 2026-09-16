package com.finsight.finsight_api.common;

import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
