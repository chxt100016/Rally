package com.rally.domain.auth.model;

import lombok.Data;

@Data
public class WechatSession {
    private String openid;
    private String unionid;
    private String sessionKey;
}
