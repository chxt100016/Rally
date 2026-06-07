package com.rally.domain.utils;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;

public interface Assert {

    static void notNull(Object obj, BizErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
    }

    static void isTrue(boolean condition, BizErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }

    static void notEmpty(String str, BizErrorCode errorCode) {
        if (str == null || str.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }


}
