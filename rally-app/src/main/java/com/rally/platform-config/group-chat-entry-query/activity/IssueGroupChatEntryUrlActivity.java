package com.rally.platformconfig.groupchatentryquery.activity;

import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import org.springframework.stereotype.Component;

/**
 * 业务活动 issue-group-chat-entry-url：签发固定群聊二维码的一小时临时访问地址。
 */
@Component
public class IssueGroupChatEntryUrlActivity {

    private static final String GROUP_CHAT_ENTRY_KEY = "default/qrcode.jpg";
    private static final long URL_TTL_SECONDS = 3600L;

    private final AssetStorageService assetStorageService;

    public IssueGroupChatEntryUrlActivity(AssetStorageGateway storageGateway) {
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    public String execute() {
        // A1 只选择 main 的固定 key，不读取 system.group.qrcode 配置。
        String resourceKey = GROUP_CHAT_ENTRY_KEY;

        // A2 原样交给对象存储签名一小时，不探测对象是否存在。
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                resourceKey, URL_TTL_SECONDS);
        if (signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发群聊入口地址失败");
        }

        // A3 直接返回 raw 签名 URL，不封装 DTO 或单独有效期。
        return signedRead.getSignedUrl();
    }
}
