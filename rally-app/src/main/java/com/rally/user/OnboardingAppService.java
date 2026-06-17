package com.rally.user;

import com.rally.utils.UserContext;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.model.MyProfileDTO;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
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

    /**
     * 查是否需引导，返回状态枚举
     * 无记录则生成 tbc
     */
    public ProfileStatusEnum checkStatus() {
        String userId = UserContext.get();
        UserProfile profile = userProfileDomainService.get(userId);
        profile.assertCompleted();

        if (ProfileStatusEnum.NONE == profile.getStatus()) {
            this.userProfileDomainService.init(profile);
            return ProfileStatusEnum.NONE;
        }
        return profile.getStatus();
    }

    /**
     * 提交 Onboarding，转 normal
     */
    @Transactional
    public MyProfileDTO submit(OnboardingCmd cmd) {
        String userId = UserContext.get();
        UserProfile profile = userProfileDomainService.get(userId);
        if (ProfileStatusEnum.NONE == profile.getStatus()) {
            this.userProfileDomainService.init(profile);
        }
        cmd.getVideos().forEach(video -> QiniuConfiguration.buildSignedUrl(video.getKey()));
        profile.completeOnboarding(cmd);
        userProfileDomainService.save(profile);

        return myProfileAppService.getMyProfile();
    }
}
