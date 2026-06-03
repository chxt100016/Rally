package com.rally.db.user.gateway;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.user.gateway.VideoUploadGateway;
import com.rally.domain.user.model.VideoTokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QiniuVideoUploadGatewayImpl implements VideoUploadGateway {

    private final QiniuClient qiniuClient;

    @Value("${qiniu.access-key}")
    private String accessKey;

    @Value("${qiniu.secret-key}")
    private String secretKey;

    @Value("${qiniu.bucket}")
    private String bucket;

    @Value("${qiniu.domain}")
    private String domain;

    @Value("${qiniu.callback-url:}")
    private String callbackUrl;

    @Override
    public VideoTokenVO generateUploadToken(String userId, int maxSizeMb) {
        // 作用域限定到 videos/{userId}/
        String scope = bucket + ":videos/" + userId + "/";
        long fsizeLimit = (long) maxSizeMb * 1024 * 1024;

        Auth auth = Auth.create(accessKey, secretKey);

        // 构建上传凭证
        // 使用 Auth.uploadToken 生成带限制的凭证
        StringMap policy = new StringMap();
        policy.put("scope", scope);
        policy.put("isPrefixalScope", 1);
        policy.put("fsizeLimit", fsizeLimit);
        policy.put("deadline", System.currentTimeMillis() / 1000 + 600);

        // 设置回调
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            policy.put("callbackUrl", callbackUrl);
            policy.put("callbackBody", "{\"key\":\"$(key)\",\"userId\":\"" + userId + "\",\"fsize\":$(fsize),\"duration\":$(avinfo.video.duration)}");
            policy.put("callbackBodyType", "application/json");
        }

        String uploadToken = auth.uploadToken(bucket, null, 3600, policy);

        VideoTokenVO vo = new VideoTokenVO();
        vo.setUploadToken(uploadToken);
        vo.setKeyPrefix("videos/" + userId + "/");
        vo.setMaxSizeMb(maxSizeMb);
        vo.setMaxDurationSec(60);
        vo.setUploadHost("https://up-z2.qiniup.com");
        return vo;
    }

    @Override
    public boolean verifyCallbackSignature(String authorization, String body) {
        if (authorization == null || authorization.isBlank()) {
            return false;
        }
        try {
            // 解析 Authorization: QBox <token> 或 Qiniu <token>
            String token;
            if (authorization.startsWith("QBox ")) {
                token = authorization.substring(5);
            } else if (authorization.startsWith("Qiniu ")) {
                token = authorization.substring(6);
            } else {
                return false;
            }

            Auth auth = Auth.create(accessKey, secretKey);
            // 验证回调签名
            String url = callbackUrl;
            byte[] bodyBytes = body != null ? body.getBytes() : new byte[0];
            String expectedToken = auth.signRequest(url, bodyBytes, "application/json");
            return token.equals(expectedToken);
        } catch (Exception e) {
            log.error("七牛回调签名验证失败", e);
            return false;
        }
    }
}
