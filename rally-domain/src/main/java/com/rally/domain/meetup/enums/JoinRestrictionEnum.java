package com.rally.domain.meetup.enums;

/**
 * 未报名场景下的准入限制原因（可叠加）
 * <p>
 * 仅在 actionState 为 JOIN_DIRECT / APPLY_APPROVAL 时计算：
 * 列表非空即表示当前用户不满足报名条件，按钮置灰并按原因展示文案（文案由前端拼装）。
 * 判断逻辑与报名接口的服务端校验同源，避免前后端规则不一致。
 */
public enum JoinRestrictionEnum {
    /** 已满员 */
    FULL,
    /** 仅限男性参加 */
    GENDER_MALE_ONLY,
    /** 仅限女性参加 */
    GENDER_FEMALE_ONLY,
    /** 性别未知 */
    GENDER_UNKNOWN,
    /** 水平不符（NTRP 不在要求范围） */
    LEVEL_NOT_MATCH,
    /** 信誉分低于门槛 */
    LOW_REPUTATION,
    ;
}
