package com.rally.domain.media.assetstorage;

/**
 * 上传授权请求。字段会按调用方提供的原始值交给对象存储适配器。
 */
public final class UploadAuthorizationRequest {

    private final UploadScopeMode scopeMode;
    private final String resourceScope;
    private final String sdkKey;
    private final long maxBytes;
    private final long policyDeadlineSeconds;
    private final long tokenTtlSeconds;

    public UploadAuthorizationRequest(UploadScopeMode scopeMode,
                                      String resourceScope,
                                      String sdkKey,
                                      long maxBytes,
                                      long policyDeadlineSeconds,
                                      long tokenTtlSeconds) {
        this.scopeMode = scopeMode;
        this.resourceScope = resourceScope;
        this.sdkKey = sdkKey;
        this.maxBytes = maxBytes;
        this.policyDeadlineSeconds = policyDeadlineSeconds;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    /**
     * 兼容旧的单期限构造方式。第三个参数不再表示服务端校验的命名空间，
     * 而是原样作为 SDK key 使用；单一期限同时用于 policy deadline 与 token TTL。
     */
    @Deprecated
    public UploadAuthorizationRequest(UploadScopeMode scopeMode,
                                      String resourceScope,
                                      String allowedNamespace,
                                      long maxBytes,
                                      long expiresInSeconds) {
        this(scopeMode, resourceScope, allowedNamespace, maxBytes,
                expiresInSeconds, expiresInSeconds);
    }

    public UploadScopeMode getScopeMode() {
        return scopeMode;
    }

    public String getResourceScope() {
        return resourceScope;
    }

    public String getSdkKey() {
        return sdkKey;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public long getPolicyDeadlineSeconds() {
        return policyDeadlineSeconds;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    /**
     * @deprecated 不再执行命名空间约束；为兼容旧调用返回 SDK key。
     */
    @Deprecated
    public String getAllowedNamespace() {
        return sdkKey;
    }

    /**
     * @deprecated 上传授权现在分别表达 policy deadline 与 token TTL。
     */
    @Deprecated
    public long getExpiresInSeconds() {
        return tokenTtlSeconds;
    }
}
