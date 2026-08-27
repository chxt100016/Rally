package com.rally.personalprofile.videouploadauthorization.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.UploadAuthorizationOutcome;
import com.rally.domain.media.assetstorage.UploadAuthorizationRequest;
import com.rally.domain.media.assetstorage.UploadAuthorizationResult;
import com.rally.domain.media.assetstorage.UploadScopeMode;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.domain.user.model.VideoVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 issue-video-upload-authorization：核对已登记数量并签发视频上传授权。
 */
@Component
public class IssueVideoUploadAuthorizationActivity {

    private static final String VIDEO_PREFIX = "videos/";
    private static final String UPLOAD_HOST = "https://up-z0.qiniup.com";
    private static final int MAX_DURATION_SECONDS = 60;
    private static final long POLICY_DEADLINE_SECONDS = 600L;
    private static final long TOKEN_TTL_SECONDS = 3600L;

    private final TennisProfileRepository tennisProfileRepository;
    private final AssetStorageService assetStorageService;

    public IssueVideoUploadAuthorizationActivity(TennisProfileRepository tennisProfileRepository,
                                                 AssetStorageGateway storageGateway) {
        this.tennisProfileRepository = tennisProfileRepository;
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    public VideoTokenVO execute(String userId) {
        TennisProfileData profile = tennisProfileRepository.findByUserId(userId).orElse(null);
        List<VideoVO> videos = profile == null ? null : profile.getVideos();

        // A1：只有已登记列表非空时才读数量配置；非法整数按 0 保留 main 语义。
        if (videos != null && !videos.isEmpty()) {
            int maxCount = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_COUNT.getKey());
            if (videos.size() >= maxCount) {
                throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED);
            }
        }

        // A2：大小配置非法时签出 0MB 授权，不额外拒绝。
        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB.getKey());
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        String keyPrefix = VIDEO_PREFIX + userId + "/";

        // A3：初始 policy 使用前缀 scope，SDK key 传 null，保留七牛重建为桶级 scope 的行为。
        UploadAuthorizationResult authorization = assetStorageService.authorizeUpload(
                new UploadAuthorizationRequest(
                        UploadScopeMode.KEY_PREFIX,
                        keyPrefix,
                        null,
                        maxBytes,
                        POLICY_DEADLINE_SECONDS,
                        TOKEN_TTL_SECONDS));
        if (authorization.getOutcome() != UploadAuthorizationOutcome.AUTHORIZED) {
            throw new IllegalStateException("签发视频上传授权失败");
        }

        VideoTokenVO result = new VideoTokenVO();
        result.setUploadToken(authorization.getUploadToken());
        result.setKeyPrefix(keyPrefix);
        result.setMaxSizeMb(maxSizeMb);
        result.setMaxDurationSec(MAX_DURATION_SECONDS);
        result.setUploadHost(UPLOAD_HOST);
        // key 与 resourceUrl 保持 null；本活动不保存授权，也不预占视频名额。
        return result;
    }
}
