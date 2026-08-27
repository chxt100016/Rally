package com.rally.domain.payment.receiptlog;

import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;

import java.util.Objects;

/**
 * 支付回执日志聚合根。
 *
 * <p>身份、渠道、类型、关联和报文在建立后没有修改入口；payment_log 的新增与
 * CALLBACK 终结都只能通过 {@link ReceiptLogPersistence} 发起。</p>
 */
public final class ReceiptLog {

    public static final String PAYMENT_LOG_IMMUTABLE = "PAYMENT_LOG_IMMUTABLE";
    public static final String PAYMENT_LOG_INITIAL_STATE_INVALID =
            "PAYMENT_LOG_INITIAL_STATE_INVALID";
    public static final String PAYMENT_LOG_STATE_CONFLICT = "PAYMENT_LOG_STATE_CONFLICT";

    private static final int BIZ_ID_MAX_LENGTH = 32;
    private static final int REF_TYPE_MAX_LENGTH = 16;
    private static final int REF_ID_MAX_LENGTH = 64;

    private ReceiptLogState state;

    private ReceiptLog(ReceiptLogState state) {
        this.state = state;
    }

    /** C1：建立不可变日志并通过唯一写端口完整插入。 */
    public static ReceiptLog record(
            RecordPaymentEventCommand command,
            ReceiptLogIdGenerator idGenerator,
            ReceiptLogPersistence persistence) {
        require(command != null, PAYMENT_LOG_IMMUTABLE, "支付事件命令不能为空");
        require(idGenerator != null, PAYMENT_LOG_IMMUTABLE, "日志编号生成器不能为空");
        require(persistence != null, PAYMENT_LOG_STATE_CONFLICT, "日志持久化端口不能为空");
        require(command.channel() != null, PAYMENT_LOG_IMMUTABLE, "支付渠道不能为空");
        require(command.logType() != null, PAYMENT_LOG_IMMUTABLE, "日志类型不能为空");

        ReceiptLogReference reference = command.reference() == null
                ? ReceiptLogReference.none() : command.reference();
        validateReference(reference);
        String bizId = idGenerator.nextBizId();
        requireNotBlank(bizId, PAYMENT_LOG_IMMUTABLE, "日志编号不能为空");
        require(bizId.length() <= BIZ_ID_MAX_LENGTH,
                PAYMENT_LOG_IMMUTABLE,
                "日志编号超过存储上限");

        PaymentLogStatusEnum initialStatus =
                command.logType() == PaymentLogTypeEnum.CALLBACK
                        ? PaymentLogStatusEnum.RECEIVED
                        : PaymentLogStatusEnum.PROCESSED;
        ReceiptLog log = new ReceiptLog(new ReceiptLogState(
                null,
                bizId,
                command.channel(),
                command.logType(),
                reference,
                command.rawBody(),
                initialStatus,
                null,
                null,
                null));
        log.checkInvariants();

        ReceiptLogInsertResult inserted = persistence.insert(log.state);
        require(inserted != null, PAYMENT_LOG_STATE_CONFLICT, "日志插入没有返回结果");
        if (inserted == ReceiptLogInsertResult.BIZ_ID_CONFLICT) {
            throw immutable("日志编号已存在");
        }
        require(inserted == ReceiptLogInsertResult.CREATED,
                PAYMENT_LOG_STATE_CONFLICT,
                "未知日志插入结果");
        log.checkInvariants();
        return log;
    }

    /** 从 payment_log 的一条完整可信记录恢复聚合。 */
    public static ReceiptLog restore(ReceiptLogState state) {
        require(state != null, PAYMENT_LOG_IMMUTABLE, "日志状态不能为空");
        ReceiptLog log = new ReceiptLog(state);
        log.checkInvariants();
        return log;
    }

    /** C2：只按 bizId 普通更新一次 CALLBACK 的处理结果。 */
    public ReceiptLogCompletion completeCallback(
            CompleteCallbackCommand command,
            ReceiptLogPersistence persistence) {
        require(command != null, PAYMENT_LOG_STATE_CONFLICT, "终结命令不能为空");
        require(persistence != null, PAYMENT_LOG_STATE_CONFLICT, "日志持久化端口不能为空");
        require(state.logType() == PaymentLogTypeEnum.CALLBACK,
                PAYMENT_LOG_STATE_CONFLICT,
                "只有渠道回调日志可以终结");
        validateConclusion(command.conclusion());

        persistence.updateConclusion(
                state.bizId(), command.conclusion(), command.remark());
        state = state.withConclusion(command.conclusion(), command.remark());
        checkInvariants();
        return ReceiptLogCompletion.FINALIZED_NOW;
    }

    public ReceiptLogState state() {
        return state;
    }

    /** 将 uk_biz_id 冲突转换成 I1 的稳定领域错误。 */
    public static ReceiptLogDomainException immutableConflict(Throwable cause) {
        return new ReceiptLogDomainException(
                PAYMENT_LOG_IMMUTABLE,
                "日志编号已存在",
                cause);
    }

    /** I1-I4：恢复以及 C1/C2 完成后校验全部聚合内不变量。 */
    private void checkInvariants() {
        requireNotBlank(state.bizId(), PAYMENT_LOG_IMMUTABLE, "日志编号不能为空");
        require(state.bizId().length() <= BIZ_ID_MAX_LENGTH,
                PAYMENT_LOG_IMMUTABLE,
                "日志编号超过存储上限");
        require(state.channel() != null, PAYMENT_LOG_IMMUTABLE, "支付渠道不能为空");
        require(state.logType() != null, PAYMENT_LOG_IMMUTABLE, "日志类型不能为空");
        validateReference(state.reference());

        require(state.processStatus() != null,
                PAYMENT_LOG_INITIAL_STATE_INVALID,
                "日志处理状态不能为空");
        if (state.logType() == PaymentLogTypeEnum.CALLBACK) {
            require(state.processStatus() == PaymentLogStatusEnum.RECEIVED
                            || isTerminal(state.processStatus()),
                    PAYMENT_LOG_INITIAL_STATE_INVALID,
                    "渠道回调处理状态非法");
        } else {
            require(state.processStatus() == PaymentLogStatusEnum.PROCESSED,
                    PAYMENT_LOG_INITIAL_STATE_INVALID,
                    "纯留痕支付事件必须建立即完成");
        }
    }

    private static void validateReference(ReceiptLogReference reference) {
        require(reference != null, PAYMENT_LOG_IMMUTABLE, "关联对象不能为空");
        if (!isBlank(reference.refType())) {
            require(Objects.equals(ReceiptLogReference.PAYMENT_ORDER, reference.refType()),
                    PAYMENT_LOG_IMMUTABLE,
                    "不支持的日志关联类型");
            require(reference.refType().length() <= REF_TYPE_MAX_LENGTH,
                    PAYMENT_LOG_IMMUTABLE,
                    "关联类型超过存储上限");
        }
        if (!isBlank(reference.refId())) {
            require(reference.refId().length() <= REF_ID_MAX_LENGTH,
                    PAYMENT_LOG_IMMUTABLE,
                    "关联编号超过存储上限");
        }
    }

    private static void validateConclusion(PaymentLogStatusEnum conclusion) {
        require(isTerminal(conclusion),
                PAYMENT_LOG_STATE_CONFLICT,
                "回调只能终结为 PROCESSED 或 FAILED");
    }

    private static boolean isTerminal(PaymentLogStatusEnum status) {
        return status == PaymentLogStatusEnum.PROCESSED
                || status == PaymentLogStatusEnum.FAILED;
    }

    private static void requireNotBlank(
            String value, String errorIdentifier, String message) {
        require(!isBlank(value), errorIdentifier, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new ReceiptLogDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }

    private static ReceiptLogDomainException immutable(String message) {
        return new ReceiptLogDomainException(PAYMENT_LOG_IMMUTABLE, message);
    }

}
