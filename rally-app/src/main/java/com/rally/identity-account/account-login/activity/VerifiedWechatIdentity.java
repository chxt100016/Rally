package com.rally.identityaccount.accountlogin.activity;

/**
 * 微信身份核实结果；unionid 允许缺失，session_key 不进入活动输出。
 */
public record VerifiedWechatIdentity(String openid, String unionid) {
}
