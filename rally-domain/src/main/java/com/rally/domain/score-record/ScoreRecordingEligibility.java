package com.rally.domain.meetup.scorerecord;

import java.util.Set;

/**
 * 保留早期生成命令签名的边界载荷。
 *
 * <p>复盘资格由调用方在进入比分服务前校验，本聚合不使用该字段拒绝录入。</p>
 */
public record ScoreRecordingEligibility(
        boolean allowed,
        Set<String> validParticipantUserIds) {
}
