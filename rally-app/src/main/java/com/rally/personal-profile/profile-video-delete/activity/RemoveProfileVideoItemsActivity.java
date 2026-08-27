package com.rally.personalprofile.profilevideodelete.activity;

import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 remove-profile-video-items：从当前用户档案移除全部同 key 视频项。
 */
@Component
@RequiredArgsConstructor
public class RemoveProfileVideoItemsActivity {

    private final UserProfileDomainService userProfileDomainService;

    public void execute(String userId, String key) {
        // A1 用户缺失由聚合读取稳定返回 TOKEN_INVALID；无档案与列表遍历异常自然传播。
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A1-A2 C7 仅校验删除前列表多于一项，再移除全部同 key 项；不要求命中。
        userProfile.deleteVideo(key);

        // A3 未命中时也保存完整原列表，供同一事务内后续物理删除继续执行。
        userProfileDomainService.save(userProfile);
    }
}
