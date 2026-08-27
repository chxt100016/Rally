package com.rally.identityaccount.accountlogin.activity;

import com.rally.domain.auth.model.LoginResultVO;
import com.rally.utils.TokenUtils;
import org.springframework.stereotype.Component;

/**
 * 业务活动 issue-login-credential：为已识别的用户签发登录凭证。
 */
@Component
public class IssueLoginCredentialActivity {

    public LoginResultVO execute(String userId, boolean isNewUser) {
        // A1/A2 复用现有签发设施读取 JWT 配置、构造 HMAC 密钥并以 userId 签发新 token。
        String token = TokenUtils.issue(userId);

        // A3 保持旧登录契约：资料完善标识严格跟随本次是否新建用户。
        return new LoginResultVO(token, userId, isNewUser, isNewUser);
    }
}
