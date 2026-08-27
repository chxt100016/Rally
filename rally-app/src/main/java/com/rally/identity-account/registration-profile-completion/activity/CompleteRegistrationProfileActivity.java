package com.rally.identityaccount.registrationprofilecompletion.activity;

import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.EditProfileCmd;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 complete-registration-profile：保存当前用户提交的注册资料。
 */
@Component
@RequiredArgsConstructor
public class CompleteRegistrationProfileActivity {

    private final UserRepository userRepository;

    public void execute(String userId, CompleteRegistrationCmd registration) {
        // A1 保留注册接口的提交语义；日期时间入参落库时只取日期。
        EditProfileCmd profileChange = new EditProfileCmd();
        profileChange.setNickname(registration.getNickname());
        profileChange.setAvatarUrl(registration.getAvatarUrl());
        profileChange.setGender(registration.getGender());
        profileChange.setBirthday(registration.getBirthday() == null
                ? null
                : registration.getBirthday().toLocalDate());
        profileChange.setCityCode(registration.getCityCode());

        // A2 只修改现存用户；空字符串仍是已提交值，null 可选项保持原值。
        UserData user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        UserProfile aggregate = UserProfile.create(user, null);
        aggregate.updateBasicProfile(profileChange);
        userRepository.updateUser(aggregate.getUser());

        // A3 无业务返回数据；正常返回即表示本次保存完成。
    }
}
