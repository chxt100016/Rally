package com.rally.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 变更原因枚举（全场景原因码，落 change_log.reason）
 * 落库为 VARCHAR(32) 小写串
 */
@Getter
@AllArgsConstructor
public enum ChangeReasonEnum {

    /** 用户手动修改 */
    USER("user"),
    /** 系统自动计算 */
    SYSTEM("system"),
    /** 遇差票（核查期重置） */
    REVIEW_BAD("review_bad"),

    // 信誉分行为加减
    /** 准时/未标记 +2 */
    ON_TIME("on_time"),
    /** 迟到 −10 */
    LATE("late"),
    /** 爽约 −25 */
    NO_SHOW("no_show"),

    // 约球取消阶梯
    /** 发布者取消（有人报名）24h 外 */
    CANCEL_24H_OUT("cancel_24h_out"),
    /** 取消 12–24h */
    CANCEL_12_24H("cancel_12_24h"),
    /** 取消 6–12h */
    CANCEL_6_12H("cancel_6_12h"),
    /** 取消 <6h */
    CANCEL_UNDER_6H("cancel_under_6h"),

    // 约球退出阶梯
    /** 报名者退出 6h 外（不扣分，仅释放名额） */
    QUIT_6H_OUT("quit_6h_out"),
    /** 报名者退出 <6h（视为爽约） */
    QUIT_UNDER_6H("quit_under_6h");

    /** 落库小写串 */
    private final String value;
}
