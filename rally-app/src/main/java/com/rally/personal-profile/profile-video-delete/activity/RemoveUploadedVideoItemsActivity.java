package com.rally.personalprofile.profilevideodelete.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 remove-uploaded-video-items：移除本人上传目录中已登记的视频项。
 */
@Component
@RequiredArgsConstructor
public class RemoveUploadedVideoItemsActivity {

    private final TennisProfileRepository tennisProfileRepository;

    public void execute(String userId, String key) {
        // A1 保留 main 的原始字符串前缀判断，且在读取档案前拒绝非本人目录。
        if (!key.startsWith("videos/" + userId + "/")) {
            throw new BusinessException(BizErrorCode.VIDEO_NOT_OWNED);
        }

        // A2 仅读取网球档案，不读取基础用户；缺失时不自动初始化。
        TennisProfileData profile = tennisProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        UserProfile userProfile = UserProfile.create(null, profile);

        // A2-A3 C7 按删除前数量校验，随后 removeIf 移除全部同 key 项，不要求命中。
        userProfile.deleteVideo(key);

        // A3 无命中时也保存原列表，且只更新完整 videos JSON。
        tennisProfileRepository.updateVideos(userId, profile.getVideos());
    }
}
