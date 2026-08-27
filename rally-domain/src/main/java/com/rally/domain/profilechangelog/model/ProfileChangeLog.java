package com.rally.domain.profilechangelog.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 用户档案变更日志聚合根。
 *
 * <p>聚合只提供追加事实的建立命令。状态快照不可变，建立后没有任何修改入口。</p>
 */
public final class ProfileChangeLog {

    public static final String PROFILE_LOG_IMMUTABLE = "PROFILE_LOG_IMMUTABLE";
    public static final String PROFILE_LOG_DELTA_INVALID = "PROFILE_LOG_DELTA_INVALID";
    public static final String PROFILE_REVIEW_TRIGGER_INVALID = "PROFILE_REVIEW_TRIGGER_INVALID";
    public static final String PROFILE_LOG_SOURCE_DUPLICATE = "PROFILE_LOG_SOURCE_DUPLICATE";

    private final ProfileChangeLogData state;

    private ProfileChangeLog(ProfileChangeLogData state) {
        this.state = state;
    }

    /** C1：记录一次 NTRP 变化。 */
    public static ProfileChangeLog recordNtrpChange(
            RecordNtrpChangeCommand command,
            ProfileChangeLogIdGenerator idGenerator,
            ProfileChangeLogSourceUniqueness sourceUniqueness) {
        require(command != null, PROFILE_LOG_DELTA_INVALID, "NTRP 变化命令不能为空");
        require(idGenerator != null, PROFILE_LOG_IMMUTABLE, "日志业务编号生成器不能为空");

        BigDecimal delta = command.beforeValue() == null
                ? BigDecimal.ZERO
                : command.afterValue() == null
                        ? null
                        : command.afterValue().subtract(command.beforeValue());
        ProfileChangeLog log = new ProfileChangeLog(ProfileChangeLogData.newRecord(
                idGenerator.nextLogBizId(),
                command.userId(),
                ProfileChangeLogType.NTRP,
                command.beforeValue(),
                command.afterValue(),
                delta,
                ProfileChangeReason.USER,
                command.remark(),
                command.refId()));
        log.checkInvariants(sourceUniqueness);
        return log;
    }

    /** C2：记录核查期触发事实。 */
    public static ProfileChangeLog recordReviewTrigger(
            RecordReviewTriggerCommand command,
            ProfileChangeLogIdGenerator idGenerator,
            ProfileChangeLogSourceUniqueness sourceUniqueness) {
        require(command != null, PROFILE_REVIEW_TRIGGER_INVALID, "核查触发命令不能为空");
        require(idGenerator != null, PROFILE_LOG_IMMUTABLE, "日志业务编号生成器不能为空");

        BigDecimal requiredMatches = command.requiredMatches() == null
                ? null
                : BigDecimal.valueOf(command.requiredMatches());
        ProfileChangeLog log = new ProfileChangeLog(ProfileChangeLogData.newRecord(
                idGenerator.nextLogBizId(),
                command.userId(),
                ProfileChangeLogType.UNDER_REVIEW,
                requiredMatches,
                requiredMatches,
                null,
                command.reason(),
                command.remark(),
                command.refId()));
        log.checkInvariants(sourceUniqueness);
        return log;
    }

    /** 从持久化状态恢复只读聚合，不提供任何改写入口。 */
    public static ProfileChangeLog restore(ProfileChangeLogData state) {
        require(state != null, PROFILE_LOG_IMMUTABLE, "日志状态不能为空");
        ProfileChangeLog log = new ProfileChangeLog(state);
        log.checkIntrinsicInvariants();
        return log;
    }

    /** 交由持久化适配器新增保存的不可变状态快照。 */
    public ProfileChangeLogData state() {
        return state;
    }

    public String bizId() {
        return state.bizId();
    }

    public String userId() {
        return state.userId();
    }

    private void checkInvariants(ProfileChangeLogSourceUniqueness sourceUniqueness) {
        checkIntrinsicInvariants();
        if (state.refId() != null) {
            require(sourceUniqueness != null, PROFILE_LOG_SOURCE_DUPLICATE, "来源幂等检查器不能为空");
            if (sourceUniqueness.exists(state.sourceKey())) {
                throw sourceDuplicate();
            }
        }
    }

    /** C1/C2 后校验 I1-I3；I4 由同一建立流程的唯一性端口继续校验。 */
    private void checkIntrinsicInvariants() {
        // I1：非空业务编号、用户编号，以及不可变状态对象共同保证事实不可改写。
        requireNotBlank(state.bizId(), PROFILE_LOG_IMMUTABLE, "日志业务编号不能为空");
        requireNotBlank(state.userId(), PROFILE_LOG_IMMUTABLE, "用户编号不能为空");
        require(state.type() != null, PROFILE_LOG_IMMUTABLE, "变更类型不能为空");
        require(state.reason() != null, PROFILE_LOG_IMMUTABLE, "变更原因不能为空");

        if (ProfileChangeLogType.NTRP == state.type()) {
            // I2：首次填写增量为 0；其他情况必须严格等于后值减前值。
            require(state.afterValue() != null, PROFILE_LOG_DELTA_INVALID, "NTRP 后值不能为空");
            BigDecimal expectedDelta = state.beforeValue() == null
                    ? BigDecimal.ZERO
                    : state.afterValue().subtract(state.beforeValue());
            require(decimalEquals(expectedDelta, state.value()),
                    PROFILE_LOG_DELTA_INVALID,
                    "NTRP 变化量与前后值不一致");
        }

        if (ProfileChangeLogType.UNDER_REVIEW == state.type()) {
            // I3：核查触发以同一正数记录起始场次和剩余场次，且必须说明触发原因。
            require(isPositive(state.beforeValue())
                            && decimalEquals(state.beforeValue(), state.afterValue())
                            && ProfileChangeReason.USER == state.reason()
                            && !isBlank(state.remark()),
                    PROFILE_REVIEW_TRIGGER_INVALID,
                    "核查触发记录不符合约束");
        }
    }

    /** 将数据库幂等唯一键冲突转换为稳定领域错误。 */
    public static ProfileChangeLogDomainException sourceDuplicate() {
        return new ProfileChangeLogDomainException(
                PROFILE_LOG_SOURCE_DUPLICATE,
                "同一来源的档案变更日志已存在");
    }

    public static ProfileChangeLogDomainException sourceDuplicate(Throwable cause) {
        return new ProfileChangeLogDomainException(
                PROFILE_LOG_SOURCE_DUPLICATE,
                "同一来源的档案变更日志已存在",
                cause);
    }

    private static boolean decimalEquals(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requireNotBlank(String value, String errorIdentifier, String message) {
        require(!isBlank(value), errorIdentifier, message);
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new ProfileChangeLogDomainException(
                    Objects.requireNonNull(errorIdentifier),
                    message);
        }
    }
}
