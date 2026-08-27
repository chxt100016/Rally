package com.rally.personalprofile.profilevideoadd.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.user.model.UploadVideoCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.model.VideoVO;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 append-profile-video：向当前用户的已有网球档案追加一项视频。
 */
@Component
@RequiredArgsConstructor
public class AppendProfileVideoActivity {

    private final UserProfileDomainService userProfileDomainService;

    public void execute(String userId, UploadVideoCmd command) {
        // A1 读取完整用户档案；用户不存在保留 TOKEN_INVALID，无档案由 C6 自然失败。
        UserProfile userProfile = userProfileDomainService.get(userId);

        // A2 只预试签名地址构建，不访问或校验资源。
        QiniuConfiguration.buildSignedUrl(command.getKey());

        // A3 原样追加 key/title；C6 在列表为 null 时建立空列表，不限量、不去重。
        VideoVO video = new VideoVO();
        video.setKey(command.getKey());
        video.setTitle(command.getTitle());
        userProfile.addVideo(video);
        userProfileDomainService.save(userProfile);
    }
}
