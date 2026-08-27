package com.rally.domain.meetup.scorerecord;

import com.rally.domain.user.enums.GenderEnum;

/** 保存比分时冻结的一位球员资料；查询未命中时展示字段可空。 */
public record ScorePlayerSnapshot(
        String userId,
        String nickname,
        String avatarKey,
        GenderEnum gender) {
}
