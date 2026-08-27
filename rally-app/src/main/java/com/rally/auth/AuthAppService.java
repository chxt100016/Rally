package com.rally.auth;

import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.identityaccount.accountlogin.activity.EstablishWechatAccountActivity;
import com.rally.identityaccount.accountlogin.activity.EstablishedWechatAccount;
import com.rally.identityaccount.accountlogin.activity.IssueLoginCredentialActivity;
import com.rally.identityaccount.accountlogin.activity.VerifiedWechatIdentity;
import com.rally.identityaccount.accountlogin.activity.VerifyWechatIdentityActivity;
import com.rally.identityaccount.registrationprofilecompletion.activity.CompleteRegistrationProfileActivity;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AuthAppService {

    @Resource
    private VerifyWechatIdentityActivity verifyWechatIdentityActivity;

    @Resource
    private EstablishWechatAccountActivity establishWechatAccountActivity;

    @Resource
    private IssueLoginCredentialActivity issueLoginCredentialActivity;

    @Resource
    private CompleteRegistrationProfileActivity completeRegistrationProfileActivity;

    public LoginResultVO login(WechatLoginCmd cmd) {
        VerifiedWechatIdentity identity = verifyWechatIdentityActivity.execute(cmd.getCode());
        EstablishedWechatAccount account = establishWechatAccountActivity.execute(
                identity.openid(), identity.unionid());
        return issueLoginCredentialActivity.execute(account.userId(), account.isNewUser());
    }


    public void completeRegistration(CompleteRegistrationCmd cmd) {
        String userId = UserContext.get();
        completeRegistrationProfileActivity.execute(userId, cmd);
    }
}
