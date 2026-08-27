package com.rally.domain.media.assetstorage;

import java.time.DateTimeException;
import java.time.Instant;

/**
 * 按 main 的原始参数签发上传、临时读取和删除能力。
 *
 * <p>本服务不追加键格式、命名空间、容量、资源所有权或对象存在性校验，
 * 也不保存授权结果。</p>
 */
public class AssetStorageService {

    private final AssetStorageGateway storageGateway;

    public AssetStorageService(AssetStorageGateway storageGateway) {
        if (storageGateway == null) {
            throw new IllegalArgumentException("storageGateway must not be null");
        }
        this.storageGateway = storageGateway;
    }

    /**
     * R1/R2/R3/R4：把初始 policy 参数和 SDK 参数分别原样交给令牌签发器。
     */
    public UploadAuthorizationResult authorizeUpload(UploadAuthorizationRequest request) {
        if (request == null
                || request.getScopeMode() == null
                || request.getResourceScope() == null) {
            return UploadAuthorizationResult.rejected();
        }

        Instant issuedAt = Instant.now();
        Instant policyDeadlineAt = deadlineFrom(issuedAt, request.getPolicyDeadlineSeconds());
        Instant tokenExpiresAt = deadlineFrom(issuedAt, request.getTokenTtlSeconds());
        if (policyDeadlineAt == null || tokenExpiresAt == null) {
            return UploadAuthorizationResult.rejected();
        }

        String uploadToken = storageGateway.issueUploadToken(
                request.getScopeMode(),
                request.getResourceScope(),
                request.getSdkKey(),
                request.getMaxBytes(),
                request.getPolicyDeadlineSeconds(),
                request.getTokenTtlSeconds());
        if (uploadToken == null || uploadToken.isBlank()) {
            return UploadAuthorizationResult.rejected();
        }
        return UploadAuthorizationResult.authorized(
                uploadToken, policyDeadlineAt, tokenExpiresAt);
    }

    /**
     * R1/R5：空白键返回空结果；其他原始键直接签名，不探测对象存在性。
     */
    public SignedReadResult signReadUrl(String resourceKey, long expiresInSeconds) {
        if (resourceKey == null || resourceKey.isBlank()) {
            return SignedReadResult.rejected();
        }

        Instant expiresAt = deadlineFrom(Instant.now(), expiresInSeconds);
        if (expiresAt == null) {
            return SignedReadResult.rejected();
        }

        String signedUrl = storageGateway.issueSignedReadUrl(resourceKey, expiresAt);
        if (signedUrl == null || signedUrl.isBlank()) {
            return SignedReadResult.rejected();
        }
        return SignedReadResult.signed(signedUrl, expiresAt);
    }

    /**
     * R1/R6：把原始键交给网关；612 由适配器映射，其余异常不在此处吞掉。
     */
    public AssetDeleteOutcome delete(String resourceKey) {
        AssetDeleteOutcome outcome = storageGateway.delete(resourceKey);
        return outcome == null ? AssetDeleteOutcome.FAILED : outcome;
    }

    private static Instant deadlineFrom(Instant issuedAt, long expiresInSeconds) {
        try {
            return issuedAt.plusSeconds(expiresInSeconds);
        } catch (DateTimeException | ArithmeticException e) {
            return null;
        }
    }
}
