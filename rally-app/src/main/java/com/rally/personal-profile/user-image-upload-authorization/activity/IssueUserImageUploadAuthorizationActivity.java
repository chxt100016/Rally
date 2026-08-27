package com.rally.personalprofile.userimageuploadauthorization.activity;

import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.media.assetstorage.UploadAuthorizationOutcome;
import com.rally.domain.media.assetstorage.UploadAuthorizationRequest;
import com.rally.domain.media.assetstorage.UploadAuthorizationResult;
import com.rally.domain.media.assetstorage.UploadScopeMode;
import com.rally.domain.user.model.VideoTokenVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 业务活动 issue-user-image-upload-authorization：按原始用途签发本人图片上传授权。
 */
@Component
public class IssueUserImageUploadAuthorizationActivity {

    private static final int MAX_SIZE_MB = 10;
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String UPLOAD_HOST = "https://up-z0.qiniup.com";
    private static final long POLICY_DEADLINE_SECONDS = 600L;
    private static final long TOKEN_TTL_SECONDS = 3600L;
    private static final long RESOURCE_URL_TTL_SECONDS = 3600L;
    private static final DateTimeFormatter KEY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final AssetStorageService assetStorageService;

    public IssueUserImageUploadAuthorizationActivity(AssetStorageGateway storageGateway) {
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    public VideoTokenVO execute(String userId, String type) {
        // A1 type 不做任何规范化或校验，保留 main 的原始字符串拼接语义。
        String timestamp = LocalDateTime.now().format(KEY_TIMESTAMP_FORMAT);
        String key = "user/" + userId + "/" + type + "_" + timestamp + ".jpg";

        // A2 以精确 key 同时限定初始 policy scope 和 SDK key，大小固定为 10MB。
        UploadAuthorizationResult authorization = assetStorageService.authorizeUpload(
                new UploadAuthorizationRequest(
                        UploadScopeMode.EXACT_KEY,
                        key,
                        key,
                        MAX_SIZE_BYTES,
                        POLICY_DEADLINE_SECONDS,
                        TOKEN_TTL_SECONDS));

        // A3 资源地址固定签发一小时；签发失败不保存任何授权或资源记录。
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                key, RESOURCE_URL_TTL_SECONDS);
        if (authorization.getOutcome() != UploadAuthorizationOutcome.AUTHORIZED
                || signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发用户图片上传授权失败");
        }

        VideoTokenVO result = new VideoTokenVO();
        result.setUploadToken(authorization.getUploadToken());
        result.setKey(key);
        result.setMaxSizeMb(MAX_SIZE_MB);
        result.setUploadHost(UPLOAD_HOST);
        result.setResourceUrl(signedRead.getSignedUrl());
        // keyPrefix 与 maxDurationSec 使用 VO 默认值 null/0。
        return result;
    }
}
