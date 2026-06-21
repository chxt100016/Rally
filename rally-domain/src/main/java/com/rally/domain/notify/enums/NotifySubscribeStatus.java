package com.rally.domain.notify.enums;

/**
 * 订阅通知流水状态
 * UNUSED  已授权待发（一条 = 一次真实的微信订阅额度）
 * SENDING 发送中（CAS 占用，防并发重复发送）
 * SENT    已发送成功
 * FAILED  发送失败
 * EXPIRED 订阅额度过期未使用
 */
public enum NotifySubscribeStatus {
    UNUSED,
    SENDING,
    SENT,
    FAILED,
    EXPIRED
}
