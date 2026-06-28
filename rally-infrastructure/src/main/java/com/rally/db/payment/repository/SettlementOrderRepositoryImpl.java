package com.rally.db.payment.repository;

import com.rally.db.payment.convert.PaymentConvertMapper;
import com.rally.db.payment.entity.SettlementOrderPO;
import com.rally.db.payment.service.SettlementOrderService;
import com.rally.domain.payment.enums.SettlementStatusEnum;
import com.rally.domain.payment.gateway.SettlementOrderRepository;
import com.rally.domain.payment.model.SettlementOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分账单网关实现
 */
@Component
@RequiredArgsConstructor
public class SettlementOrderRepositoryImpl implements SettlementOrderRepository {

    private final SettlementOrderService settlementOrderService;
    private static final PaymentConvertMapper MAPPER = PaymentConvertMapper.INSTANCE;

    @Override
    public void save(SettlementOrder order) {
        settlementOrderService.save(MAPPER.toSettlementPO(order.getData()));
    }

    @Override
    public SettlementOrder findByBizId(String bizId) {
        SettlementOrderPO po = settlementOrderService.lambdaQuery().eq(SettlementOrderPO::getBizId, bizId).one();
        return toOrder(po);
    }

    @Override
    public SettlementOrder findByPaymentOrderId(String paymentOrderId) {
        SettlementOrderPO po = settlementOrderService.lambdaQuery().eq(SettlementOrderPO::getPaymentOrderId, paymentOrderId).one();
        return toOrder(po);
    }

    @Override
    public List<SettlementOrder> listProcessing() {
        return settlementOrderService.lambdaQuery().eq(SettlementOrderPO::getStatus, SettlementStatusEnum.PROCESSING.name()).list().stream().map(this::toOrder).toList();
    }

    @Override
    public boolean hasProcessingByPayee(String payeeUserId) {
        return settlementOrderService.lambdaQuery().eq(SettlementOrderPO::getPayeeUserId, payeeUserId).eq(SettlementOrderPO::getStatus, SettlementStatusEnum.PROCESSING.name()).count() > 0;
    }

    @Override
    public boolean markProcessing(String bizId, String channelOrderId) {
        return settlementOrderService.lambdaUpdate().eq(SettlementOrderPO::getBizId, bizId).eq(SettlementOrderPO::getStatus, SettlementStatusEnum.PENDING.name()).set(SettlementOrderPO::getStatus, SettlementStatusEnum.PROCESSING.name()).set(SettlementOrderPO::getChannelOrderId, channelOrderId).update();
    }

    @Override
    public boolean markFinished(String bizId, LocalDateTime finishTime) {
        return settlementOrderService.lambdaUpdate().eq(SettlementOrderPO::getBizId, bizId).eq(SettlementOrderPO::getStatus, SettlementStatusEnum.PROCESSING.name()).set(SettlementOrderPO::getStatus, SettlementStatusEnum.FINISHED.name()).set(SettlementOrderPO::getFinishTime, finishTime).update();
    }

    @Override
    public boolean markFailed(String bizId, String reason) {
        return settlementOrderService.lambdaUpdate().eq(SettlementOrderPO::getBizId, bizId).eq(SettlementOrderPO::getStatus, SettlementStatusEnum.PROCESSING.name()).set(SettlementOrderPO::getStatus, SettlementStatusEnum.FAILED.name()).set(SettlementOrderPO::getFailReason, reason).update();
    }

    private SettlementOrder toOrder(SettlementOrderPO po) {
        return po == null ? null : new SettlementOrder(MAPPER.toSettlementData(po));
    }
}
