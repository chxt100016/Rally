package com.rally.user;

import com.rally.utils.UserContext;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.model.MyProfileDTO;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.personalprofile.initialprofilesubmission.activity.CompleteInitialProfileActivity;
import com.rally.personalprofile.onboardingstatus.activity.InspectOnboardingStatusActivity;
import com.rally.personalprofile.onboardingstatus.activity.InitializePendingProfileActivity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OnboardingAppService {

    @Resource
    private UserProfileDomainService userProfileDomainService;

    @Resource
    private MyProfileAppService myProfileAppService;

    @Resource
    private CompleteInitialProfileActivity completeInitialProfileActivity;

    @Resource
    private InitializePendingProfileActivity initializePendingProfileActivity;

    @Resource
    private InspectOnboardingStatusActivity inspectOnboardingStatusActivity;

    /**
     * 查是否需引导，返回状态枚举
     * 无记录则生成 tbc
     */
    public ProfileStatusEnum checkStatus() {
        String userId = UserContext.get();
        ProfileStatusEnum status = inspectOnboardingStatusActivity.execute();

        if (ProfileStatusEnum.NONE == status) {
            UserProfile profile = userProfileDomainService.get(userId);
            initializePendingProfileActivity.execute(profile);
            // A3 保留本次查询开始时的 NONE，不重新读取已写入的 TBC。
            return ProfileStatusEnum.NONE;
        }
        return status;
    }

    /**
     * 提交 Onboarding，转 normal
     */
    @Transactional
    public MyProfileDTO submit(OnboardingCmd cmd) {
        String userId = UserContext.get();
        completeInitialProfileActivity.execute(userId, cmd);

        return myProfileAppService.getMyProfile();
    }
}
