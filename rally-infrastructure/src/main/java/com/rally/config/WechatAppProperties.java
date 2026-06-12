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
}
