package com.rally.domain.meetup.scorerecord;

/** 比分记录聚合违反领域契约时抛出的异常，保留稳定错误标识。 */
public final class ScoreRecordDomainException extends RuntimeException {

    private final String errorIdentifier;

    public ScoreRecordDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public ScoreRecordDomainException(
            String errorIdentifier,
            String message,
            Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
