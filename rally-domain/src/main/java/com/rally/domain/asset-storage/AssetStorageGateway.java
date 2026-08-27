package com.rally.domain.media.assetstorage;

import java.time.Instant;

/**
 * 对象存储网关，由基础设施层适配具体对象存储 SDK。
 *
 * <p>上传参数按 main 的调用顺序原样传递：先构造 policy，再把可空 sdkKey 和独立 TTL
 * 交给 SDK。SDK 是否重建 scope 或 deadline 由供应商实现决定。</p>
 */
public interface AssetStorageGateway {

    /**
     * 签发上传令牌。无法签发的预期失败返回空值；未被适配器归类的基础设施异常可向上抛出。
     */
    String issueUploadToken(UploadScopeMode policyScopeMode,
                            String policyResourceScope,
                            String sdkKey,
                            long maxBytes,
                            long policyDeadlineSeconds,
                            long tokenTtlSeconds);

    /**
     * 为指定键签发读取地址，不探测对象是否存在。无法签发的预期失败返回空值。
     */
    String issueSignedReadUrl(String resourceKey, Instant expiresAt);

    /**
     * 幂等删除对象。适配器必须将供应商的“对象不存在”响应映射为 ALREADY_ABSENT。
     */
    AssetDeleteOutcome delete(String resourceKey);
}
