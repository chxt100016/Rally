package com.rally.domain.system.platformconfig;

/** sys_config 唯一插入的确定性结果。 */
public record PlatformConfigInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        BIZ_ID_CONFLICT,
        IDENTITY_CONFLICT
    }

    public static PlatformConfigInsertResult created(long generatedId) {
        return new PlatformConfigInsertResult(Outcome.CREATED, generatedId);
    }

    public static PlatformConfigInsertResult bizIdConflict() {
        return new PlatformConfigInsertResult(Outcome.BIZ_ID_CONFLICT, null);
    }

    public static PlatformConfigInsertResult identityConflict() {
        return new PlatformConfigInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
