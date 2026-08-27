package com.rally.identityaccount.accountlogin.activity;

import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.gateway.WechatClient;
import com.rally.domain.auth.model.WechatSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 业务活动 verify-wechat-identity：用小程序临时凭据核实微信身份。
 */
@Component
@RequiredArgsConstructor
public class VerifyWechatIdentityActivity {

    private final WechatClient wechatClient;

    public VerifiedWechatIdentity execute(String code) {
        // A1/A2 配置检查、固定 code2session 参数与 GET 请求由既有微信网关统一完成。
        WechatSession session = wechatClient.code2Session(code);

        // A3 网关已校验微信错误码；活动再守住有效 openid，且不向下游暴露 session_key。
        if (session == null || StringUtils.isBlank(session.getOpenid())) {
            throw new AuthException(10001, "微信 code 无效: null response");
        }
        return new VerifiedWechatIdentity(session.getOpenid(), session.getUnionid());
    }
}
