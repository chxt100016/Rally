package com.rally.upload;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rally.client.qiniu.QiniuClient;
import com.rally.utils.UserContext;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.domain.user.model.VideoVO;
import com.rally.domain.utils.Assert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class VideoAppService {

    @Resource
    private TennisProfileRepository tourProfileRepository;

    @Resource
    private QiniuClient qiniuClient;

    public VideoTokenVO getVideoUploadToken() {
        String userId = UserContext.get();
        TennisProfileData profileData = tourProfileRepository.findByUserId(userId).orElse(null);
        if (profileData != null) {
            int maxCount = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_COUNT.getKey());
            List<VideoVO> currentVideos = profileData.getVideos();
            if (currentVideos != null && currentVideos.size() >= maxCount) {
                throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED);
            }
        }

        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB.getKey());
        return this.generateUploadToken(userId, maxSizeMb);
    }

    @Transactional
    public void deleteVideo(String key) {
        String userId = UserContext.get();
        if (!key.startsWith("videos/" + userId + "/")) {
            throw new BusinessException(BizErrorCode.VIDEO_NOT_OWNED);
        }

        TennisProfileData profileData = tourProfileRepository.findByUserId(userId).orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        List<VideoVO> videos = profileData.getVideos();
        if (videos == null) {
            videos = new ArrayList<>();
        }

        Assert.isTrue(videos.size() > 1, BizErrorCode.VIDEO_AT_LEAST_ONE);

        videos.removeIf(video -> video.getKey().equals(key));
        tourProfileRepository.updateVideos(userId, videos);

        qiniuClient.deleteFile(key);
    }

    public VideoTokenVO getAvatarUploadToken(String ext) {
        String userId = UserContext.get();
        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_AVATAR_MAX_SIZE_MB.getKey());
        return this.generateAvatarUploadToken(userId, ext, maxSizeMb);
    }

    public VideoTokenVO getUserFileUploadToken(String type) {
        String userId = UserContext.get();
        int maxSizeMb = 10;
        return this.generateUserFileUploadToken(userId, type, maxSizeMb);
    }

    private VideoTokenVO generateUserFileUploadToken(String userId, String type, int maxSizeMb) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String key = "user/" + userId + "/" + type + "_" + timestamp + ".jpg";
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

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

    private VideoTokenVO generateAvatarUploadToken(String userId, String ext, int maxSizeMb) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String key = "avatar/" + userId + "_" + timestamp + "." + ext;
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

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
        String scope = QiniuConfiguration.getBucket() + ":videos/" + userId + "/";
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

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
