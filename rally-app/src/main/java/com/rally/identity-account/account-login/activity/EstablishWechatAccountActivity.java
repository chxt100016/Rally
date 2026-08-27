package com.rally.identityaccount.accountlogin.activity;

import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.auth.model.AccountData;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.UserConst;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 业务活动 establish-wechat-account：识别微信小程序账户，首次登录时建立默认用户与账户。
 */
@Component
@RequiredArgsConstructor
public class EstablishWechatAccountActivity {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public EstablishedWechatAccount execute(String openid, String unionid) {
        // A1 只以微信小程序渠道和 openid 识别账户；命中后不回查用户或回填 unionid。
        Optional<AccountData> existingAccount = accountRepository.findByChannelAndIdentifier(
                ChannelEnum.WECHAT_MINIAPP,
                openid);
        if (existingAccount.isPresent()) {
            return new EstablishedWechatAccount(existingAccount.get().getUserId(), false);
        }

        // A2 保持既有首登行为：先建默认用户，不与随后的账户建立共享活动级事务。
        UserData newUser = new UserData();
        newUser.setNickname(UserConst.DEFAULT_NICKNAME);
        newUser.setAvatarUrl(UserConst.DEFAULT_AVATAR_URL);
        newUser.setGender(GenderEnum.UNDISCLOSED);
        UserData savedUser = userRepository.createUser(newUser);

        // A3 唯一键冲突和其他写入失败原样上抛；不回查重试，也不补偿已建用户。
        AccountData newAccount = new AccountData();
        newAccount.setUserId(savedUser.getUserId());
        newAccount.setChannel(ChannelEnum.WECHAT_MINIAPP);
        newAccount.setIdentifier(openid);
        newAccount.setUnionId(unionid);
        accountRepository.createAccount(newAccount);

        // A4 只有本次同时新建用户和账户才标记为新用户。
        return new EstablishedWechatAccount(savedUser.getUserId(), true);
    }
}
