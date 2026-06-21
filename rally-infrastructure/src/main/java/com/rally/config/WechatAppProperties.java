package com.rally.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wechat.mini")
public class WechatAppProperties {
    private String appId;
    private String secret;
    private String code2sessionUrl;
    private String accessTokenUrl;
    private String subscribeSendUrl;
    /** 订阅消息跳转版本: developer(开发版) / trial(体验版) / formal(正式版, 默认) */
    private String subscribeMiniprogramState;
}
