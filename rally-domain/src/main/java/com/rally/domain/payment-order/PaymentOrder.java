package com.rally.domain.payment.paymentorder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 支付单聚合根。
 *
 * <p>支付身份和金额建立后没有修改入口。C1-C5 的表写入全部通过
 * {@link PaymentOrderPersistence} 发起。离开 PENDING 的写入由数据库条件更新兜底，
 * 但聚合保持 main 的弱并发语义：内存结论取决于加载时状态，不依据影响行数补查。</p>
 */
public final class PaymentOrder {

    public static final String PAYMENT_IDENTITY_INVALID = "PAYMENT_IDENTITY_INVALID";
    public static final String PAYMENT_AMOUNT_INVALID = "PAYMENT_AMOUNT_INVALID";
    public static final String PAYMENT_ACTIVE_CONFLICT = "PAYMENT_ACTIVE_CONFLICT";
    public static final String PAYMENT_RESULT_INVALID = "PAYMENT_RESULT_INVALID";
    public static final String PAYMENT_STATE_CONFLICT = "PAYMENT_STATE_CONFLICT";

    private PaymentOrderState state;

    private PaymentOrder(PaymentOrderState state) {
        this.state = state;
    }

    /** C1：计算不可变金额和活跃键，建立并插入一张 PENDING 支付单。 */
    public static PaymentOrder create(
            CreatePaymentOrderCommand command,
            PaymentOrderIdGenerator idGenerator,
            PaymentOrderPersistence persistence) {
        require(command != null, PAYMENT_IDENTITY_INVALID, "建单命令不能为空");
        require(idGenerator != null, PAYMENT_IDENTITY_INVALID, "支付编号生成器不能为空");
        require(persistence != null, PAYMENT_STATE_CONFLICT, "支付单持久化端口不能为空");
        requireNotBlank(command.channel(), PAYMENT_IDENTITY_INVALID, "支付渠道不能为空");
        requireNotBlank(command.bizType(), PAYMENT_IDENTITY_INVALID, "业务类型不能为空");
        requireNotBlank(command.refBizId(), PAYMENT_IDENTITY_INVALID, "业务引用不能为空");
        requireNotBlank(command.payerUserId(), PAYMENT_IDENTITY_INVALID, "付款人不能为空");
        require(command.createdAt() != null, PAYMENT_IDENTITY_INVALID, "建单时间不能为空");
        require(command.expireTime() == null || command.expireTime().isAfter(command.createdAt()),
                PAYMENT_STATE_CONFLICT,
                "支付超时时间必须晚于建单时间");

        int feeAmount = calculateFee(command.baseAmount(), command.feeRate());
        int payAmount;
        try {
            payAmount = Math.addExact(command.baseAmount(), feeAmount);
        } catch (ArithmeticException e) {
            throw error(PAYMENT_AMOUNT_INVALID, "实付金额超出整数范围", e);
        }

        String bizId = idGenerator.nextBizId();
        requireNotBlank(bizId, PAYMENT_IDENTITY_INVALID, "支付编号不能为空");
        String activeRefKey = buildActiveRefKey(
                command.bizType(), command.refBizId(), command.payerUserId());
        PaymentOrderState created = new PaymentOrderState(
                null,
                bizId,
                command.channel(),
                command.bizType(),
                command.refBizId(),
                command.payerUserId(),
                command.baseAmount(),
                feeAmount,
                payAmount,
                PaymentOrderStatus.PENDING,
                null,
                null,
                null,
                activeRefKey,
                command.description(),
                null,
                command.expireTime(),
                command.createdAt(),
                command.createdAt());
        PaymentOrder order = new PaymentOrder(created);
        order.checkInvariants();

        PaymentOrderInsertResult inserted = persistence.insert(created);
        require(inserted != null, PAYMENT_STATE_CONFLICT, "支付单插入没有返回结果");
        switch (inserted) {
            case CREATED -> {
                order.checkInvariants();
                return order;
            }
            case BIZ_ID_CONFLICT -> throw error(PAYMENT_IDENTITY_INVALID, "支付编号已存在");
            case ACTIVE_REF_CONFLICT -> throw activeConflict();
            default -> throw error(PAYMENT_STATE_CONFLICT, "未知支付单插入结果");
        }
    }

    /** 从数据库的一张完整记录恢复聚合。 */
    public static PaymentOrder restore(PaymentOrderState state) {
        require(state != null, PAYMENT_IDENTITY_INVALID, "支付单状态不能为空");
        PaymentOrder order = new PaymentOrder(state);
        order.checkInvariants();
        return order;
    }

    /** C2：任意已加载状态均按 bizId 普通更新调用方原样给出的预支付资料。 */
    public void savePrepay(SavePrepayCommand command, PaymentOrderPersistence persistence) {
        require(command != null, PAYMENT_RESULT_INVALID, "保存预支付凭证命令不能为空");
        requirePersistence(persistence);

        persistence.savePrepay(state.bizId(), command.prepayId(), command.prepayExpireTime());
        state = state.withPrepay(command.prepayId(), command.prepayExpireTime());
        checkInvariants();
    }

    /** C3：按加载时状态判断首次支付；PENDING 条件更新的影响行数不改变该结论。 */
    public PaymentOrderConfirmation confirmPaid(
            ConfirmPaymentCommand command, PaymentOrderPersistence persistence) {
        requirePersistence(persistence);
        if (state.status() == PaymentOrderStatus.PAID) {
            checkInvariants();
            return PaymentOrderConfirmation.ALREADY_PAID;
        }
        require(state.status() == PaymentOrderStatus.PENDING,
                PAYMENT_STATE_CONFLICT,
                "关闭或失败的支付单不能确认支付");
        require(command != null, PAYMENT_RESULT_INVALID, "渠道支付结果不能为空");
        require(command.channelPaidAt() != null, PAYMENT_RESULT_INVALID, "本地确认时间不能为空");

        persistence.markPaidIfPending(
                state.bizId(), command.channelTransactionId(), command.channelPaidAt());
        state = state.asPaid(command.channelTransactionId(), command.channelPaidAt());
        checkInvariants();
        return PaymentOrderConfirmation.FIRST_PAID;
    }

    /** C4：关闭资格由调用活动判断；按加载时状态返回关闭或幂等结论。 */
    public PaymentOrderClosure close(
            ClosePaymentCommand command, PaymentOrderPersistence persistence) {
        requirePersistence(persistence);
        if (state.status() == PaymentOrderStatus.CLOSED) {
            checkInvariants();
            return PaymentOrderClosure.ALREADY_CLOSED;
        }
        require(state.status() == PaymentOrderStatus.PENDING,
                PAYMENT_STATE_CONFLICT,
                "已支付或失败的支付单不能关闭");
        require(command != null, PAYMENT_STATE_CONFLICT, "关闭支付单命令不能为空");

        persistence.closeIfPending(state.bizId());
        state = state.asClosed();
        checkInvariants();
        return PaymentOrderClosure.CLOSED_NOW;
    }

    /** C5：以单条 PENDING CAS 写入失败摘要、FAILED 状态并释放活跃键。 */
    public void markCreationFailed(
            FailPaymentOrderCommand command, PaymentOrderPersistence persistence) {
        require(command != null, PAYMENT_RESULT_INVALID, "标记建单失败命令不能为空");
        requirePersistence(persistence);
        require(state.status() == PaymentOrderStatus.PENDING,
                PAYMENT_STATE_CONFLICT,
                "只有待支付订单可以标记建单失败");

        persistence.failIfPending(state.bizId(), command.failureSummary());
        state = state.asFailed(command.failureSummary());
        checkInvariants();
    }

    /** 预支付复用必须同时满足 PENDING、凭证非空且当前时间严格早于有效期。 */
    public boolean isPrepayReusableAt(LocalDateTime currentTime) {
        require(currentTime != null, PAYMENT_RESULT_INVALID, "当前时间不能为空");
        return state.status() == PaymentOrderStatus.PENDING
                && !isBlank(state.prepayId())
                && state.prepayExpireTime() != null
                && currentTime.isBefore(state.prepayExpireTime());
    }

    public PaymentOrderState state() {
        return state;
    }

    public static String buildActiveRefKey(String bizType, String refBizId, String payerUserId) {
        requireNotBlank(bizType, PAYMENT_IDENTITY_INVALID, "业务类型不能为空");
        requireNotBlank(refBizId, PAYMENT_IDENTITY_INVALID, "业务引用不能为空");
        requireNotBlank(payerUserId, PAYMENT_IDENTITY_INVALID, "付款人不能为空");
        return bizType + ":" + refBizId + ":" + payerUserId;
    }

    /** 把数据库 uk_active_ref 冲突转换为 I3 的稳定领域错误。 */
    public static PaymentOrderDomainException activeConflict() {
        return error(PAYMENT_ACTIVE_CONFLICT, "相同业务、付款人已有活跃支付单");
    }

    public static PaymentOrderDomainException activeConflict(Throwable cause) {
        return error(PAYMENT_ACTIVE_CONFLICT, "相同业务、付款人已有活跃支付单", cause);
    }

    private void checkInvariants() {
        // I1：支付与业务身份完整，且状态快照只有定向 copy 方法，没有身份修改入口。
        requireNotBlank(state.bizId(), PAYMENT_IDENTITY_INVALID, "支付编号不能为空");
        requireNotBlank(state.channel(), PAYMENT_IDENTITY_INVALID, "支付渠道不能为空");
        requireNotBlank(state.bizType(), PAYMENT_IDENTITY_INVALID, "业务类型不能为空");
        requireNotBlank(state.refBizId(), PAYMENT_IDENTITY_INVALID, "业务引用不能为空");
        requireNotBlank(state.payerUserId(), PAYMENT_IDENTITY_INVALID, "付款人不能为空");

        // I2：金额为正、手续费非负，实付额必须精确等于基础额与手续费之和。
        require(state.baseAmount() > 0, PAYMENT_AMOUNT_INVALID, "基础金额必须为正");
        require(state.feeAmount() >= 0, PAYMENT_AMOUNT_INVALID, "手续费不能为负");
        try {
            require(Math.addExact(state.baseAmount(), state.feeAmount()) == state.payAmount(),
                    PAYMENT_AMOUNT_INVALID,
                    "实付金额必须等于基础金额与手续费之和");
        } catch (ArithmeticException e) {
            throw error(PAYMENT_AMOUNT_INVALID, "实付金额超出整数范围", e);
        }

        require(state.status() != null, PAYMENT_STATE_CONFLICT, "支付状态不能为空");
        boolean active = state.status() == PaymentOrderStatus.PENDING
                || state.status() == PaymentOrderStatus.PAID;
        String expectedActiveRef = buildActiveRefKey(
                state.bizType(), state.refBizId(), state.payerUserId());
        // I3：活跃状态占用确定键，关闭或失败在同一次状态变更中清键。
        require(active
                        ? Objects.equals(expectedActiveRef, state.activeRefKey())
                        : state.activeRefKey() == null,
                PAYMENT_ACTIVE_CONFLICT,
                "活跃键与支付状态不一致");

        boolean hasPayTime = state.payTime() != null;
        // I4：PAID 必须有本地确认时间；渠道流水按调用方结果原样记录，允许为空。
        require(state.status() == PaymentOrderStatus.PAID
                        ? hasPayTime
                        : state.channelTransactionId() == null && !hasPayTime,
                PAYMENT_RESULT_INVALID,
                "支付状态与渠道支付结果不一致");

        // I5：所有终态都只能由本类的 PENDING CAS 命令建立；恢复时也拒绝不完整终态。
        if (state.status() == PaymentOrderStatus.CLOSED
                || state.status() == PaymentOrderStatus.FAILED) {
            require(state.activeRefKey() == null,
                    PAYMENT_STATE_CONFLICT,
                    "支付终态必须已经释放活跃键");
        }
    }

    private static int calculateFee(int baseAmount, BigDecimal feeRate) {
        require(baseAmount > 0, PAYMENT_AMOUNT_INVALID, "基础金额必须为正");
        require(feeRate != null && feeRate.signum() >= 0,
                PAYMENT_AMOUNT_INVALID,
                "手续费率不能为空或为负");
        try {
            return BigDecimal.valueOf(baseAmount)
                    .multiply(feeRate)
                    .setScale(0, RoundingMode.CEILING)
                    .intValueExact();
        } catch (ArithmeticException e) {
            throw error(PAYMENT_AMOUNT_INVALID, "手续费超出整数范围", e);
        }
    }

    private static void requirePersistence(PaymentOrderPersistence persistence) {
        require(persistence != null, PAYMENT_STATE_CONFLICT, "支付单持久化端口不能为空");
    }

    private static void requireNotBlank(String value, String identifier, String message) {
        require(!isBlank(value), identifier, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void require(boolean condition, String identifier, String message) {
        if (!condition) {
            throw error(identifier, message);
        }
    }

    private static PaymentOrderDomainException error(String identifier, String message) {
        return new PaymentOrderDomainException(identifier, message);
    }

    private static PaymentOrderDomainException error(
            String identifier, String message, Throwable cause) {
        return new PaymentOrderDomainException(identifier, message, cause);
    }
}
