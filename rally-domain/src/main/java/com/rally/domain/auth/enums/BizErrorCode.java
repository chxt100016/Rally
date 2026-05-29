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
    WECHAT_AUTH_FAILED(30002, "微信授权失败");

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
