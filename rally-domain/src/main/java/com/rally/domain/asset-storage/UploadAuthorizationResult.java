package com.rally.domain.media.assetstorage;

import java.time.Instant;

/**
 * 上传授权签发结果。分别记录初始 policy 截止时间与 SDK 令牌截止时间。
 */
public final class UploadAuthorizationResult {

    private final UploadAuthorizationOutcome outcome;
    private final String uploadToken;
    private final Instant policyDeadlineAt;
    private final Instant tokenExpiresAt;

    private UploadAuthorizationResult(UploadAuthorizationOutcome outcome,
                                      String uploadToken,
                                      Instant policyDeadlineAt,
                                      Instant tokenExpiresAt) {
        this.outcome = outcome;
        this.uploadToken = uploadToken;
        this.policyDeadlineAt = policyDeadlineAt;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public static UploadAuthorizationResult authorized(String uploadToken,
                                                       Instant policyDeadlineAt,
                                                       Instant tokenExpiresAt) {
        return new UploadAuthorizationResult(UploadAuthorizationOutcome.AUTHORIZED,
                uploadToken, policyDeadlineAt, tokenExpiresAt);
    }

    /** 兼容旧的单期限结果。 */
    @Deprecated
    public static UploadAuthorizationResult authorized(String uploadToken, Instant expiresAt) {
        return new UploadAuthorizationResult(
                UploadAuthorizationOutcome.AUTHORIZED, uploadToken, expiresAt, expiresAt);
    }

    public static UploadAuthorizationResult rejected() {
        return new UploadAuthorizationResult(UploadAuthorizationOutcome.REJECTED,
                null, null, null);
    }

    public UploadAuthorizationOutcome getOutcome() {
        return outcome;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    public Instant getPolicyDeadlineAt() {
        return policyDeadlineAt;
    }

    public Instant getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    /** @deprecated 上传授权现在分别表达两个截止时间。 */
    @Deprecated
    public Instant getExpiresAt() {
        return tokenExpiresAt;
    }
}
