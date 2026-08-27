package com.rally.platformconfig.splashcoverquery.activity;

import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import org.springframework.stereotype.Component;

/**
 * 业务活动 issue-splash-cover-url：签发当前启动封面的一小时临时访问地址。
 */
@Component
public class IssueSplashCoverUrlActivity {

    private static final long URL_TTL_SECONDS = 3600L;

    private final AssetStorageService assetStorageService;

    public IssueSplashCoverUrlActivity(AssetStorageGateway storageGateway) {
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    public String execute() {
        // A1 保留 SystemConfig 的已启用 global 缓存优先级与枚举默认值回退。
        String resourceKey = SystemConfig.getString(
                SystemConfigKey.SYSTEM_SPLASH_COVER_KEY.getKey());

        // A2 已启用的空白覆盖值直接交付 null，不再回退默认图。
        if (resourceKey == null || resourceKey.isBlank()) {
            return null;
        }

        // A2 将 raw key 签名 3600 秒，不探测对象是否存在。
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                resourceKey, URL_TTL_SECONDS);
        if (signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发启动封面地址失败");
        }

        // A3 直接交付 raw URL 字符串，不封装 DTO、不返回期限也不落库。
        return signedRead.getSignedUrl();
    }
}
