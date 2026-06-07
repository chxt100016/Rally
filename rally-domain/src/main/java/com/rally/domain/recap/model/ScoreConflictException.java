package com.rally.domain.recap.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import lombok.Getter;

/**
 * 比分版本冲突异常
 * <p>
 * 冲突时携带服务端最新比分和版本号，供上层捕获处理。
 */
@Getter
public class ScoreConflictException extends BusinessException {

    private final Integer latestVersion;

    public ScoreConflictException(Integer latestVersion) {
        super(BizErrorCode.RECAP_SCORE_CONFLICT);
        this.latestVersion = latestVersion;
    }
}
