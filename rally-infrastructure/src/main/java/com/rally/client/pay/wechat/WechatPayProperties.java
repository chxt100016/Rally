package com.rally.client.pay.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置（见设计 §16）。
 * <p>
 * 全 V3 方案：JSAPI 下单 / 关单 / 查单 / 回调验签解密
 * 全部走 APIv3，商户号为「微信支付公钥」模式，签名使用
 * {@code apiV3Key + merchantSerialNumber + privateKeyPath}，验签使用 {@code publicKeyId + publicKeyPath}，
 * 证书/私钥/公钥文件不入库、不进 git，运行时由环境变量注入路径。
 */
@Data
@Component
@ConfigurationProperties("wechat.pay")
public class WechatPayProperties {
    /** 商户号 */
    private String mchId;
    /** 小程序 AppID（复用 wechat.mini.app-id，下单/接收方 add 都要带） */
    private String appId;
    /** APIv3 密钥（32 位，商户平台 → API 安全 → 设置 APIv3 密钥），下单签名 + 回调 AEAD 解密 */
    private String apiV3Key;
    /** 商户证书序列号（apiclient_cert.pem 的 serialNumber），V3 请求签名头 Wechatpay-Serial */
    private String merchantSerialNumber;
    /** 商户 API 私钥路径（apiclient_key.pem），V3 签名用 */
    private String privateKeyPath;
    /** 微信支付公钥路径（商户平台 API 安全 → 微信支付公钥下载），验证微信侧应答/回调签名用 */
    private String publicKeyPath;
    /** 微信支付公钥 ID（PUB_KEY_ID_xxx，与公钥文件同页展示） */
    private String publicKeyId;
    /** 支付回调地址（我方域名 + /api/rally/wechat/pay/notify，HTTPS 公网可达） */
    private String payNotifyUrl;
}
