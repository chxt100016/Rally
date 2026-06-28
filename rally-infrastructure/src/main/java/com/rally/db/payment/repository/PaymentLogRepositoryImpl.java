package com.rally.db.payment.repository;

import com.rally.db.payment.convert.PaymentConvertMapper;
import com.rally.db.payment.entity.PaymentLogPO;
import com.rally.db.payment.service.PaymentLogService;
import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.model.PaymentLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付全链路留痕网关实现
 */
@Component
@RequiredArgsConstructor
public class PaymentLogRepositoryImpl implements PaymentLogRepository {

    private final PaymentLogService paymentLogService;
    private static final PaymentConvertMapper MAPPER = PaymentConvertMapper.INSTANCE;

    @Override
    public void save(PaymentLog log) {
        paymentLogService.save(MAPPER.toLogPO(log.getData()));
    }

    @Override
    public void update(PaymentLog log) {
        PaymentLogPO po = paymentLogService.lambdaQuery().eq(PaymentLogPO::getBizId, log.getData().getBizId()).one();
        if (po == null) {
            return;
        }
        PaymentLogPO update = MAPPER.toLogPO(log.getData());
        update.setId(po.getId());
        paymentLogService.updateById(update);
    }

    @Override
    public List<PaymentLog> listUnprocessedCallback(LocalDateTime before) {
        return paymentLogService.lambdaQuery().eq(PaymentLogPO::getLogType, PaymentLogTypeEnum.CALLBACK.name()).eq(PaymentLogPO::getProcessStatus, PaymentLogStatusEnum.RECEIVED.name()).lt(PaymentLogPO::getCreateTime, before).list().stream().map(po -> new PaymentLog(MAPPER.toLogData(po))).toList();
    }
}
