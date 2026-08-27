package com.rally.domain.media.assetstorage;

import java.time.Instant;

/**
 * 临时读取地址签发结果。签发成功不代表对象已经存在。
 */
public final class SignedReadResult {

    private final SignedReadOutcome outcome;
    private final String signedUrl;
    private final Instant expiresAt;

    private SignedReadResult(SignedReadOutcome outcome, String signedUrl, Instant expiresAt) {
        this.outcome = outcome;
        this.signedUrl = signedUrl;
        this.expiresAt = expiresAt;
    }

    public static SignedReadResult signed(String signedUrl, Instant expiresAt) {
        return new SignedReadResult(SignedReadOutcome.SIGNED, signedUrl, expiresAt);
    }

    public static SignedReadResult rejected() {
        return new SignedReadResult(SignedReadOutcome.REJECTED, null, null);
    }

    public SignedReadOutcome getOutcome() {
        return outcome;
    }

    public String getSignedUrl() {
        return signedUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
