package com.rally.personalprofile.profilevideoupdate.activity;

import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 update-profile-video-title：修改当前用户首个同 key 档案视频的标题。
 */
@Component
@RequiredArgsConstructor
public class UpdateProfileVideoTitleActivity {

    private final UserProfileDomainService userProfileDomainService;

    public void execute(String userId, String key, String title) {
        // A1 用户缺失保留 TOKEN_INVALID；有用户无档案由 C8 自然失败。
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A2 C8 保留 main 的列表顺序和 video.key.equals(key) 语义，仅改首个命中项。
        userProfile.updateVideo(key, title);

        // A3 videos 为 null 或 key 未命中时仍保存完整档案，title 不做清洗。
        userProfileDomainService.save(userProfile);
    }
}
