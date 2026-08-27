package com.rally.identityaccount.phonebinding.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 bind-user-phone：以微信授权手机号覆盖当前用户手机号。
 */
@Component
@RequiredArgsConstructor
public class BindUserPhoneActivity {

    private final UserRepository userRepository;

    @Transactional
    public void execute(String userId, String phoneNumber) {
        // A1 保持旧接口的参数与用户不存在错误语义，不自动建立用户或账户。
        if (StringUtils.isBlank(phoneNumber)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR);
        }
        UserData user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.USER_NOT_EXIST));

        // A2 手机号只能通过 @identity.user 的 C3 命令覆盖；不比较旧值，也不检查跨用户唯一性。
        UserProfile aggregate = UserProfile.create(user, null);
        aggregate.bindAuthorizedPhone(phoneNumber);
        userRepository.updateUser(aggregate.getUser());

        // A3 无业务返回值；方法正常返回即表示绑定完成。
    }
}
