package com.rally.domain.utils;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;

import java.util.Objects;

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

    static void eq(Object o1, Object o2, BizErrorCode errorCode) {
        if (!Objects.equals(o1, o2)) {
            throw new BusinessException(errorCode);
        }
    }


}
