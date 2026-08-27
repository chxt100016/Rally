package com.rally.identityaccount.accountlogin.activity;

/**
 * 微信账户识别或建立结果。
 */
public record EstablishedWechatAccount(String userId, boolean isNewUser) {
}
