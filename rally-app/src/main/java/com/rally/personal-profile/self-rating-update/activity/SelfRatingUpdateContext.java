package com.rally.personalprofile.selfratingupdate.activity;

import java.math.BigDecimal;

/**
 * 自评修改结果，供后续核查触发日志与 NTRP 变更日志活动使用。
 */
public record SelfRatingUpdateContext(
        String userId,
        BigDecimal oldNtrp,
        BigDecimal newNtrp,
        BigDecimal delta,
        int requiredMatches) {
}
