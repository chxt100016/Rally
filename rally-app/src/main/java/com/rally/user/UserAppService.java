package com.rally.user;

import com.rally.domain.auth.gateway.WechatClient;
import com.rally.domain.auth.model.WechatPhoneInfo;
import com.rally.domain.user.service.UserDomainService;
import com.rally.user.model.WechatPhoneCodeCmd;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private final WechatClient wechatClient;
    private final UserDomainService userDomainService;

    public void saveWechatPhone(WechatPhoneCodeCmd cmd) {
        String userId = UserContext.get();
        WechatPhoneInfo phoneInfo = wechatClient.getPhoneNumber(cmd.getCode());
        userDomainService.updatePhone(userId, phoneInfo.getPhoneNumber());
    }
}
