package com.rally.personalprofile.onboardingstatus.activity;

import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 inspect-onboarding-status：读取查询开始时的本人引导状态。
 */
@Component
@RequiredArgsConstructor
public class InspectOnboardingStatusActivity {

    private final UserProfileDomainService userProfileDomainService;

    public ProfileStatusEnum execute() {
        // A1 从登录上下文定位基础用户；用户不存在时沿用 TOKEN_INVALID。
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A2 仅返回读取时状态；无档案映射 NONE，建档由后续活动编排。
        return userProfile.getStatus();
    }
}
