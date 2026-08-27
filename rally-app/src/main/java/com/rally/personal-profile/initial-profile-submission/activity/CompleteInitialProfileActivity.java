package com.rally.personalprofile.initialprofilesubmission.activity;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 complete-initial-profile：初始化并完整覆盖当前用户的初始网球档案。
 */
@Component
@RequiredArgsConstructor
public class CompleteInitialProfileActivity {

    private final UserProfileDomainService userProfileDomainService;

    public void execute(String userId, OnboardingCmd command) {
        // A1 读取当前用户及档案；用户不存在时保留 TOKEN_INVALID 契约。
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A2 NONE 时先按旧流程落库 TBC；已有任意档案状态均继续。
        if (ProfileStatusEnum.NONE == userProfile.getStatus()) {
            userProfileDomainService.init(userProfile);
        }

        // A3 只预试每项 key 的签名构址；null 项保持自然异常，不增加数量或归属校验。
        command.getVideos().forEach(video -> QiniuConfiguration.buildSignedUrl(video.getKey()));

        // A4 完整覆盖 NTRP/视频、置 NORMAL 并重置三项初始评分；性别、生日和核查字段不参与。
        userProfile.completeInitialProfile(
                command.getNtrpScore(),
                command.getVideos(),
                SystemConfig.getInt(SystemConfigKey.SCORE_INIT_REPUTATION.getKey()),
                SystemConfig.getInt(SystemConfigKey.SCORE_INIT_CREDIBILITY.getKey()),
                SystemConfig.getInt(SystemConfigKey.SCORE_INIT_CALIBRATION.getKey()),
                IdWorker::getIdStr,
                null);
        userProfileDomainService.save(userProfile);
    }
}
