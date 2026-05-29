package com.rally.domain.auth.exception;

import com.rally.domain.auth.enums.BizErrorCode;

/**
 * 通用业务异常
 */
public class BusinessException extends RuntimeException {

    private final BizErrorCode errorCode;

    public BusinessException(BizErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BizErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BizErrorCode getErrorCode() {
        return errorCode;
    }
}
