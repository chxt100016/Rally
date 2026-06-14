package com.rally.upload;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rally.utils.UserContext;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.domain.user.model.VideoVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VideoAppService {

    @Resource
    private TennisProfileGateway tennisProfileGateway;

    /**
     * 获取视频直传凭证
     */
    public VideoTokenVO getVideoUploadToken() {
        String userId = UserContext.get();
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElse(null);
        if (profileData != null) {
            int maxCount = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_COUNT);
            List<VideoVO> currentVideos = profileData.getVideos();
            if (currentVideos != null && currentVideos.size() >= maxCount) {
                throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED);
            }
        }

        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB);
        return this.generateUploadToken(userId, maxSizeMb);
    }


    /**
     * 删除视频
     */
    @Transactional
    public void deleteVideo(String key) {
        String userId = UserContext.get();
        // 校验 key 前缀
        if (!key.startsWith("videos/" + userId + "/")) {
            throw new BusinessException(BizErrorCode.VIDEO_NOT_OWNED);
        }

        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        List<VideoVO> videos = profileData.getVideos();
        if (videos == null) {
            videos = new ArrayList<>();
        }

        videos.removeIf(video -> video.getKey().equals(key));
        tennisProfileGateway.updateVideos(userId, videos);
    }


    /**
     * 获取头像直传凭证
     * @param ext 文件扩展名，如 jpg、png
     */
    public VideoTokenVO getAvatarUploadToken(String ext) {
        String userId = UserContext.get();
        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_AVATAR_MAX_SIZE_MB);
        return this.generateAvatarUploadToken(userId, ext, maxSizeMb);
    }

    private VideoTokenVO generateAvatarUploadToken(String userId, String ext, int maxSizeMb) {
        // 固定 key，每次上传覆盖：avatar/{userId}.{ext}
        String key = "avatar/" + userId + "." + ext;
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

        // 固定 key 模式，不设 isPrefixalScope
        StringMap policy = new StringMap();
        policy.put("scope", QiniuConfiguration.getBucket() + ":" + key);
        policy.put("fsizeLimit", fsizeLimit);
        policy.put("deadline", System.currentTimeMillis() / 1000 + 600);

        String uploadToken = auth.uploadToken(QiniuConfiguration.getBucket(), key, 3600, policy);

        VideoTokenVO vo = new VideoTokenVO();
        vo.setUploadToken(uploadToken);
        vo.setKey(key);
        vo.setMaxSizeMb(maxSizeMb);
        vo.setUploadHost("https://up-z0.qiniup.com");
        vo.setResourceUrl(QiniuConfiguration.buildSignedUrl(key));
        return vo;
    }

    private VideoTokenVO generateUploadToken(String userId, int maxSizeMb) {
        // 作用域限定到 videos/{userId}/
        String scope = QiniuConfiguration.getBucket() + ":videos/" + userId + "/";
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

        // 构建上传凭证
        // 使用 Auth.uploadToken 生成带限制的凭证
        StringMap policy = new StringMap();
        policy.put("scope", scope);
        policy.put("isPrefixalScope", 1);
        policy.put("fsizeLimit", fsizeLimit);
        policy.put("deadline", System.currentTimeMillis() / 1000 + 600);

        String uploadToken = auth.uploadToken(QiniuConfiguration.getBucket(), null, 3600, policy);


        VideoTokenVO vo = new VideoTokenVO();
        vo.setUploadToken(uploadToken);
        vo.setKeyPrefix("videos/" + userId + "/");
        vo.setMaxSizeMb(maxSizeMb);
        vo.setMaxDurationSec(60);
        vo.setUploadHost("https://up-z0.qiniup.com");
        return vo;
    }
}
