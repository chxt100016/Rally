package com.rally.identityaccount.phonebinding.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.auth.gateway.WechatClient;
import com.rally.domain.auth.model.WechatPhoneInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 业务活动 resolve-authorized-phone：用微信动态令牌取得本次授权手机号。
 */
@Component
@RequiredArgsConstructor
public class ResolveAuthorizedPhoneActivity {

    private final WechatClient wechatClient;

    public String execute(String code) {
        // A1/A2 由既有微信网关完成配置校验、access token 取得与敏感 POST 请求，并保留原错误码分流。
        WechatPhoneInfo phoneInfo = wechatClient.getPhoneNumber(code);

        // A3 仅交付微信确认的 phoneNumber；网关异常返回时统一按原手机号失败码拒绝。
        if (phoneInfo == null || StringUtils.isBlank(phoneInfo.getPhoneNumber())) {
            throw new BusinessException(BizErrorCode.WECHAT_PHONE_NUMBER_FAILED);
        }
        return phoneInfo.getPhoneNumber();
    }
}
