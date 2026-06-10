package com.rally.domain.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 * 前缀 1xxxx 表示认证相关错误
 * 前缀 2xxxx 表示业务相关错误
 */
@Getter
@AllArgsConstructor
public enum BizErrorCode {

    // ========== 认证相关 10001-19999 ==========
    UNAUTHORIZED(10001, "未登录，请先登录"),
    TOKEN_EXPIRED(10002, "登录已过期，请重新登录"),
    TOKEN_INVALID(10003, "登录凭证无效，请重新登录"),
    ACCESS_DENIED(10004, "无权限访问"),

    // ========== 通用业务 20001-29999 ==========
    PARAM_ERROR(20001, "参数错误"),
    DATA_NOT_FOUND(20002, "数据不存在"),
    DATA_DUPLICATE(20003, "数据已存在"),
    OPERATION_FAILED(20004, "操作失败"),

    // ========== 微信相关 30001-39999 ==========
    WECHAT_LOGIN_FAILED(30001, "微信登录失败"),
    WECHAT_AUTH_FAILED(30002, "微信授权失败"),

    // ========== 用户域 40001-40999 ==========
    ONBOARDING_INCOMPLETE(40001, "请先完善网球档案"),
    NTRP_COOLDOWN(40002, "自评修改冷却中"),
    VIDEO_LIMIT_EXCEEDED(40003, "视频数量已达上限"),
    VIDEO_NOT_OWNED(40004, "无权操作该视频"),
    NTRP_INVALID_VALUE(40005, "自评分值非法"),
    UNDER_REVIEW_LOCKED(40006, "核查期内不可进行该操作"),
    PROFILE_NOT_FOUND(40007, "档案不存在"),
    VIDEO_CALLBACK_INVALID(40008, "视频回调校验失败"),

    // ========== 约球域 41001-41999 ==========
    MEETUP_NOT_FOUND(41001, "约球不存在"),
    MEETUP_FULL(41002, "约球已满员"),
    MEETUP_EXPIRED(41003, "约球已开始或已结束，无法操作"),
    MEETUP_CLOSED(41004, "约球已关闭"),
    MEETUP_STATUS_ILLEGAL(41005, "约球状态不允许该操作"),
    CITY_CHANGE_FORBIDDEN(41019, "已发布的约球不可修改城市"),
    LOCATION_TIME_CHANGE_FORBIDDEN(41020, "已有参与者报名，不可修改时间、地点、持续时长"),
    JOIN_FORBIDDEN(41006, "当前不可报名"),
    ALREADY_JOINED(41007, "你已报名该约球"),
    NOT_JOINED(41008, "你未报名该约球"),
    TIME_CONFLICT(41009, "同一时间段已有其他约球，无法报名"),
    GENDER_NOT_MATCH(41010, "性别不符合该约球要求"),
    EDIT_LOCKED(41011, "临近开始，约球信息已锁定不可编辑"),
    PUBLISH_LIMIT_EXCEEDED(41012, "今日发布已达上限"),
    LOW_REPUTATION_BANNED(41013, "信誉分过低，暂时无法报名"),
    WAITLIST_NOT_PENDING(41014, "该报名当前状态不可撤回"),
    WAITLIST_NOT_FOUND(41015, "报名记录不存在"),
    NOT_CREATOR(41016, "仅发布者可操作"),
    CITY_NOT_OPENED(41017, "该城市暂未开通"),
    CANNOT_JOIN_OWN(41018, "不能报名自己发布的约球"),

    // ========== 评价域 42001-42999 ==========
    REVIEW_DUPLICATE(42001, "重复评价"),
    REVIEW_DEADLINE_PASSED(42002, "评价已超期"),
    REVIEW_NOT_PARTICIPANT(42003, "非本场参与者"),
    REVIEW_SELF_FORBIDDEN(42004, "不可评价自己"),
    MEETUP_NOT_FINISHED(42005, "约球未结束"),
    SCORE_FORMAT_INVALID(42010, "比分不合赛制"),
    SCORE_VERSION_CONFLICT(42011, "比分已被其他参与者更新，请刷新后重试"),
    SCORE_PLAYER_INVALID(42012, "比分选手非法"),
    SCORE_SET_DUPLICATE(42013, "盘号重复"),

    // ========== 球场域 44001-44999 ==========
    COURT_NOT_FOUND(44001, "球场不存在"),

    // ========== 赛后收集域 43001-43999 ==========
    RECAP_NOT_FOUND(43001, "赛后收集数据不存在"),
    RECAP_SCORE_CONFLICT(43002, "比分已被其他参与者更新，请刷新后重试"),
    RECAP_REVIEW_INVALID_TYPE(43003, "评价类型不合法"),
    RECAP_REVIEW_INVALID_VALUE(43004, "评价值不合法");

    private final int code;
    private final String message;

}
