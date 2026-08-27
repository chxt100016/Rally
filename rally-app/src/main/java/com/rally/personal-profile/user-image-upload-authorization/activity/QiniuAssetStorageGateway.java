package com.rally.personalprofile.userimageuploadauthorization.activity;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.media.assetstorage.AssetDeleteOutcome;
import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.UploadScopeMode;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 将对象存储领域端口适配到 main 既有的七牛参数语义。
 */
@Component
public class QiniuAssetStorageGateway implements AssetStorageGateway {

    private final Configuration configuration = new Configuration(Region.autoRegion());

    @Override
    public String issueUploadToken(UploadScopeMode policyScopeMode,
                                   String policyResourceScope,
                                   String sdkKey,
                                   long maxBytes,
                                   long policyDeadlineSeconds,
                                   long tokenTtlSeconds) {
        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());

        StringMap policy = new StringMap();
        policy.put("scope", QiniuConfiguration.getBucket() + ":" + policyResourceScope);
        if (policyScopeMode == UploadScopeMode.KEY_PREFIX) {
            policy.put("isPrefixalScope", 1);
        }
        policy.put("fsizeLimit", maxBytes);
        policy.put("deadline", System.currentTimeMillis() / 1000 + policyDeadlineSeconds);

        return auth.uploadToken(
                QiniuConfiguration.getBucket(), sdkKey, tokenTtlSeconds, policy);
    }

    @Override
    public String issueSignedReadUrl(String resourceKey, Instant expiresAt) {
        // 当前所有调用均为一小时；复用 main helper 可保留域名、协议与原始 key 行为。
        return QiniuConfiguration.buildSignedUrl(resourceKey);
    }

    @Override
    public AssetDeleteOutcome delete(String resourceKey) {
        Auth auth = Auth.create(QiniuConfiguration.getAccessKey(), QiniuConfiguration.getSecretKey());
        BucketManager bucketManager = new BucketManager(auth, configuration);
        try {
            bucketManager.delete(QiniuConfiguration.getBucket(), resourceKey);
            return AssetDeleteOutcome.DELETED;
        } catch (QiniuException exception) {
            if (exception.code() == 612) {
                return AssetDeleteOutcome.ALREADY_ABSENT;
            }
            throw new RuntimeException("七牛云删除文件失败: " + resourceKey, exception);
        }
    }
}
