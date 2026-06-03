package com.rally.domain.auth.enums;

/**
 * 业务错误码枚举
 * 前缀 1xxxx 表示认证相关错误
 * 前缀 2xxxx 表示业务相关错误
 */
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
    VIDEO_CALLBACK_INVALID(40008, "视频回调校验失败");

    private final int code;
    private final String message;

    BizErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
