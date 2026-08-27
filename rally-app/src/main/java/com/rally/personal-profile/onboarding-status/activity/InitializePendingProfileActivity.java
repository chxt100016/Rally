package com.rally.personalprofile.onboardingstatus.activity;

import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 initialize-pending-profile：为首次查询到的无档案用户初始化待完善档案。
 */
@Component
@RequiredArgsConstructor
public class InitializePendingProfileActivity {

    private final UserProfileDomainService userProfileDomainService;

    public void execute(UserProfile userProfile) {
        // A1/A2 交由 C4 构造空视频、TBC 档案并持久化；并发唯一键与保存异常原样传播。
        userProfileDomainService.init(userProfile);
    }
}
