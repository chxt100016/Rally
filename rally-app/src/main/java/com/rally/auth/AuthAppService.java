package com.rally.auth;

import com.rally.auth.convert.AuthConvertMapper;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.auth.gateway.WechatClient;
import com.rally.domain.auth.model.AccountData;
import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.auth.model.LoginResultVO;
import com.rally.domain.auth.model.WechatLoginCmd;
import com.rally.domain.auth.model.WechatSession;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.enums.UserConst;
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
    private WechatClient wechatClient;

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private UserRepository userRepository;

    public LoginResultVO login(WechatLoginCmd cmd) {
        if (StringUtils.isBlank(cmd.getCode())) {
            throw new AuthException(10001, "code 不能为空");
        }

        WechatSession session = wechatClient.code2Session(cmd.getCode());

        Optional<AccountData> accountOpt = accountRepository.findByChannelAndIdentifier(ChannelEnum.WECHAT_MINIAPP, session.getOpenid());

        String userId;
        boolean isNewUser;

        if (accountOpt.isPresent()) {
            userId = accountOpt.get().getUserId();
            isNewUser = false;
        } else {
            UserData newUser = new UserData();
            newUser.setAvatarUrl(UserConst.DEFAULT_AVATAR_URL);
            newUser.setNickname(UserConst.DEFAULT_NICKNAME);
            UserData savedUser = userRepository.createUser(newUser);
            userId = savedUser.getUserId();

            AccountData account = new AccountData();
            account.setUserId(userId);
            account.setChannel(ChannelEnum.WECHAT_MINIAPP);
            account.setIdentifier(session.getOpenid());
            account.setUnionId(session.getUnionid());
            accountRepository.createAccount(account);

            isNewUser = true;
        }

        String token = TokenUtils.issue(userId);
        return new LoginResultVO(token, userId, isNewUser, isNewUser);
    }


    public void completeRegistration(CompleteRegistrationCmd cmd) {
        String userId = UserContext.get();
        UserData userData = AuthConvertMapper.INSTANCE.toUserData(cmd);
        userData.setUserId(userId);
        userRepository.updateUser(userData);
    }
}
