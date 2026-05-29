package com.rally.client.wechat;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.config.WechatMiniappProperties;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.gateway.WechatGateway;
import com.rally.domain.auth.model.WechatSession;
import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatMiniappClient implements WechatGateway {

    private final WechatMiniappProperties properties;

    @Override
    public WechatSession code2Session(String code) {
        Code2SessionResponse resp = Http.uri(properties.getCode2sessionUrl())
                .param("appid", properties.getAppid())
                .param("secret", properties.getSecret())
                .param("js_code", code)
                .param("grant_type", "authorization_code")
                .doGet()
                .result(Code2SessionResponse.class);

        if (resp == null || resp.getErrcode() != 0 || StringUtils.isBlank(resp.getOpenid())) {
            String errmsg = resp != null ? resp.getErrmsg() : "null response";
            log.warn("code2Session 失败: errcode={}, errmsg={}", resp != null ? resp.getErrcode() : -1, errmsg);
            throw new AuthException(10001, "微信 code 无效: " + errmsg);
        }

        WechatSession session = new WechatSession();
        session.setOpenid(resp.getOpenid());
        session.setUnionid(resp.getUnionid());
        session.setSessionKey(resp.getSessionKey());
        return session;
    }

    @Data
    private static class Code2SessionResponse {
        private String openid;
        @JSONField(name = "session_key")
        private String sessionKey;
        private String unionid;
        private int errcode;
        private String errmsg;
    }
}
