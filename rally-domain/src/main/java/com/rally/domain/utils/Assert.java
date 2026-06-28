package com.rally.domain.utils;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;

import java.util.Arrays;
import java.util.Objects;

public interface Assert {

    static void notNull(Object obj, BizErrorCode errorCode) {
        if (obj == null) {
            throw new BusinessException(errorCode);
        }
    }

    static void notBlank(String str, BizErrorCode errorCode) {
        if (str == null || str.trim().isEmpty()) {
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

    static void in(Object o1, BizErrorCode errorCode, Object... objects) {
        if (Objects.isNull(o1)) {
            throw new BusinessException(errorCode);
        }
        if (Objects.isNull(objects)) {
            throw new BusinessException(errorCode);
        }
        if (Arrays.stream(objects).noneMatch(item -> item.equals(o1))) {
            throw new BusinessException(errorCode);
        }

    }


}
