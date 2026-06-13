package com.rally.auth;

import com.rally.auth.convert.AuthConvertMapper;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.gateway.AccountGateway;
import com.rally.domain.auth.gateway.WechatGateway;
import com.rally.domain.auth.model.AccountData;
import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.domain.auth.model.WechatSession;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserData;
import com.rally.utils.TokenUtils;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthAppService {

    @Resource
    private WechatGateway wechatGateway;

    @Resource
    private AccountGateway accountGateway;

    @Resource
    private UserGateway userGateway;

    public LoginResultVO login(WechatLoginCmd cmd) {
        if (StringUtils.isBlank(cmd.getCode())) {
            throw new AuthException(10001, "code 不能为空");
        }

        WechatSession session = wechatGateway.code2Session(cmd.getCode());

        Optional<AccountData> accountOpt = accountGateway.findByChannelAndIdentifier(ChannelEnum.WECHAT_MINIAPP, session.getOpenid());

        String userId;
        boolean isNewUser;

        if (accountOpt.isPresent()) {
            userId = accountOpt.get().getUserId();
            isNewUser = false;
        } else {
            UserData newUser = new UserData();
            UserData savedUser = userGateway.createUser(newUser);
            userId = savedUser.getUserId();

            AccountData account = new AccountData();
            account.setUserId(userId);
            account.setChannel(ChannelEnum.WECHAT_MINIAPP);
            account.setIdentifier(session.getOpenid());
            account.setUnionId(session.getUnionid());
            accountGateway.createAccount(account);

            isNewUser = true;
        }

        boolean needCompleteInfo = needCompleteInfo(userId);
        String token = TokenUtils.issue(userId);
        return new LoginResultVO(token, userId, isNewUser, needCompleteInfo);
    }

    private boolean needCompleteInfo(String userId) {
        return userGateway.findByUserId(userId)
                .map(user -> StringUtils.isBlank(user.getNickname()) || StringUtils.isBlank(user.getAvatarUrl()))
                .orElse(true);
    }

    public void completeRegistration(CompleteRegistrationCmd cmd) {
        String userId = UserContext.get();
        UserData userData = AuthConvertMapper.INSTANCE.toUserData(cmd);
        userData.setUserId(userId);
        userGateway.updateUser(userData);
    }
}
