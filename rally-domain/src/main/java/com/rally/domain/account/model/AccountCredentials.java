package com.rally.domain.account.model;

/** 渠道认证资料；微信跨应用标识允许为空。 */
public record AccountCredentials(String credential, String unionId) {
}
