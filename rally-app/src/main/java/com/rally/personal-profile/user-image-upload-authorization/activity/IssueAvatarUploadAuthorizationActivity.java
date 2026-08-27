package com.rally.personalprofile.userimageuploadauthorization.activity;

import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.media.assetstorage.UploadAuthorizationOutcome;
import com.rally.domain.media.assetstorage.UploadAuthorizationRequest;
import com.rally.domain.media.assetstorage.UploadAuthorizationResult;
import com.rally.domain.media.assetstorage.UploadScopeMode;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.model.VideoTokenVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务活动 issue-avatar-upload-authorization：签发本人头像的精确 key 上传授权。
 */
@Component
public class IssueAvatarUploadAuthorizationActivity {

    private static final String DEFAULT_EXTENSION = "jpeg";
    private static final String UPLOAD_HOST = "https://up-z0.qiniup.com";
    private static final long POLICY_DEADLINE_SECONDS = 600L;
    private static final long TOKEN_TTL_SECONDS = 3600L;
    private static final long RESOURCE_URL_TTL_SECONDS = 3600L;
    private static final DateTimeFormatter KEY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AssetStorageService assetStorageService;

    public IssueAvatarUploadAuthorizationActivity(AssetStorageGateway storageGateway) {
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    /**
     * 当前头像入口固定传 jpeg；用户编号由调用入口从登录上下文取得。
     */
    public VideoTokenVO execute(String userId) {
        return execute(userId, DEFAULT_EXTENSION);
    }

    /**
     * 保留 main 的原始扩展名拼接语义，不附加格式或路径校验。
     */
    public VideoTokenVO execute(String userId, String extension) {
        // A1 缺失时 SystemConfig 使用枚举默认 5，非法整数保持 main 的 0。
        int maxSizeMb = SystemConfig.getInt(SystemConfigKey.USER_AVATAR_MAX_SIZE_MB.getKey());

        // A2 秒级 key；同一用户同秒请求自然复用同一对象位置。
        String timestamp = LocalDateTime.now().format(KEY_TIMESTAMP_FORMAT);
        String key = "avatar/" + userId + "_" + timestamp + "." + extension;
        long maxBytes = (long) maxSizeMb * 1024 * 1024;

        // A3 精确 key 同时作为初始 policy scope 与 SDK key；两个期限保持独立。
        UploadAuthorizationResult authorization = assetStorageService.authorizeUpload(
                new UploadAuthorizationRequest(
                        UploadScopeMode.EXACT_KEY,
                        key,
                        key,
                        maxBytes,
                        POLICY_DEADLINE_SECONDS,
                        TOKEN_TTL_SECONDS));
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                key, RESOURCE_URL_TTL_SECONDS);
        if (authorization.getOutcome() != UploadAuthorizationOutcome.AUTHORIZED
                || signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发头像上传授权失败");
        }

        VideoTokenVO result = new VideoTokenVO();
        result.setUploadToken(authorization.getUploadToken());
        result.setKey(key);
        result.setMaxSizeMb(maxSizeMb);
        result.setUploadHost(UPLOAD_HOST);
        result.setResourceUrl(signedRead.getSignedUrl());
        // keyPrefix 与 maxDurationSec 使用 VO 默认值 null/0，保持原接口契约。
        return result;
    }
}
