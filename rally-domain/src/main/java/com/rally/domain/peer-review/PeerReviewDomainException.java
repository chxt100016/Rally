package com.rally.domain.meetup.peerreview;

/** 同场评价集合违反领域契约时抛出的异常，保留稳定错误标识。 */
public final class PeerReviewDomainException extends RuntimeException {

    private final String errorIdentifier;

    public PeerReviewDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public PeerReviewDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
