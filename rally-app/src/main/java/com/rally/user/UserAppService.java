package com.rally.user;

import com.rally.identityaccount.phonebinding.activity.BindUserPhoneActivity;
import com.rally.identityaccount.phonebinding.activity.ResolveAuthorizedPhoneActivity;
import com.rally.user.model.WechatPhoneCodeCmd;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private final ResolveAuthorizedPhoneActivity resolveAuthorizedPhoneActivity;
    private final BindUserPhoneActivity bindUserPhoneActivity;

    public void saveWechatPhone(WechatPhoneCodeCmd cmd) {
        String userId = UserContext.get();
        String phoneNumber = resolveAuthorizedPhoneActivity.execute(cmd.getCode());
        bindUserPhoneActivity.execute(userId, phoneNumber);
    }
}
