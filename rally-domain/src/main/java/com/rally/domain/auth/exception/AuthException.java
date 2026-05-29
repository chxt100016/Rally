package com.rally.domain.auth.exception;

import com.rally.domain.auth.enums.BizErrorCode;

public class AuthException extends RuntimeException {

    private final BizErrorCode errorCode;
    private final int code;

    public AuthException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
    }

    public AuthException(int code, String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
    }

    public BizErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return code;
    }
}
