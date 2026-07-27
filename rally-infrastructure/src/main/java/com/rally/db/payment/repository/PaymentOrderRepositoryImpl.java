package com.rally.db.payment.repository;

import com.rally.db.payment.convert.PaymentConvertMapper;
import com.rally.db.payment.entity.PaymentOrderPO;
import com.rally.db.payment.service.PaymentOrderService;
import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.enums.PaymentStatusEnum;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单网关实现
 */
@Component
@RequiredArgsConstructor
public class PaymentOrderRepositoryImpl implements PaymentOrderRepository {

    private final PaymentOrderService paymentOrderService;
    private static final PaymentConvertMapper MAPPER = PaymentConvertMapper.INSTANCE;

    @Override
    public void saveBatch(List<PaymentOrder> orders) {
        List<PaymentOrderPO> poList = orders.stream().map(o -> MAPPER.toOrderPO(o.getData())).toList();
        paymentOrderService.saveBatch(poList);
    }

    @Override
    public PaymentOrder findByBizId(String bizId) {
        PaymentOrderPO po = paymentOrderService.lambdaQuery().eq(PaymentOrderPO::getBizId, bizId).one();
        return toOrder(po);
    }

    @Override
    public PaymentOrder findActiveByRef(BizTypeEnum bizType, String refBizId, String payerUserId) {
        String activeRefKey = PaymentOrder.buildActiveRefKey(bizType, refBizId, payerUserId);
        PaymentOrderPO po = paymentOrderService.lambdaQuery().eq(PaymentOrderPO::getActiveRefKey, activeRefKey).one();
        return toOrder(po);
    }

    @Override
    public void updatePrepay(String bizId, String prepayId, LocalDateTime prepayExpireTime) {
        paymentOrderService.lambdaUpdate().eq(PaymentOrderPO::getBizId, bizId).set(PaymentOrderPO::getPrepayId, prepayId).set(PaymentOrderPO::getPrepayExpireTime, prepayExpireTime).update();
    }

    @Override
    public List<PaymentOrder> listPendingByPayer(String payerUserId) {
        return paymentOrderService.lambdaQuery().eq(PaymentOrderPO::getPayerUserId, payerUserId).eq(PaymentOrderPO::getStatus, PaymentStatusEnum.PENDING.name()).list().stream().map(this::toOrder).toList();
    }

    @Override
    public List<PaymentOrder> listExpiredPending(LocalDateTime now) {
        return paymentOrderService.lambdaQuery().eq(PaymentOrderPO::getStatus, PaymentStatusEnum.PENDING.name()).isNotNull(PaymentOrderPO::getExpireTime).lt(PaymentOrderPO::getExpireTime, now).list().stream().map(this::toOrder).toList();
    }

    @Override
    public boolean markPaid(String bizId, String transactionId, LocalDateTime payTime) {
        return paymentOrderService.lambdaUpdate().eq(PaymentOrderPO::getBizId, bizId).eq(PaymentOrderPO::getStatus, PaymentStatusEnum.PENDING.name()).set(PaymentOrderPO::getStatus, PaymentStatusEnum.PAID.name()).set(PaymentOrderPO::getChannelTransactionId, transactionId).set(PaymentOrderPO::getPayTime, payTime).update();
    }

    @Override
    public boolean close(String bizId) {
        return paymentOrderService.lambdaUpdate().eq(PaymentOrderPO::getBizId, bizId).eq(PaymentOrderPO::getStatus, PaymentStatusEnum.PENDING.name()).set(PaymentOrderPO::getStatus, PaymentStatusEnum.CLOSED.name()).set(PaymentOrderPO::getActiveRefKey, null).update();
    }

    @Override
    public boolean markFailed(String bizId, String reason) {
        return paymentOrderService.lambdaUpdate().eq(PaymentOrderPO::getBizId, bizId).eq(PaymentOrderPO::getStatus, PaymentStatusEnum.PENDING.name()).set(PaymentOrderPO::getStatus, PaymentStatusEnum.FAILED.name()).set(PaymentOrderPO::getActiveRefKey, null).set(PaymentOrderPO::getDescription, reason).update();
    }

    private PaymentOrder toOrder(PaymentOrderPO po) {
        return po == null ? null : new PaymentOrder(MAPPER.toOrderData(po));
    }
}
