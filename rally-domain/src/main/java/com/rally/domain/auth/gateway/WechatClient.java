package com.rally.domain.auth.gateway;

import com.rally.domain.auth.model.WechatSession;

public interface WechatClient {
    WechatSession code2Session(String code);
}
