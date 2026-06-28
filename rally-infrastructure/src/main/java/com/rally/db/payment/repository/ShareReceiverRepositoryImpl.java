package com.rally.db.payment.repository;

import com.rally.db.payment.convert.PaymentConvertMapper;
import com.rally.db.payment.entity.ShareReceiverPO;
import com.rally.db.payment.service.ShareReceiverService;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.ShareReceiverStatusEnum;
import com.rally.domain.payment.gateway.ShareReceiverRepository;
import com.rally.domain.payment.model.ShareReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 分账接收方账本网关实现
 */
@Component
@RequiredArgsConstructor
public class ShareReceiverRepositoryImpl implements ShareReceiverRepository {

    private final ShareReceiverService shareReceiverService;
    private static final PaymentConvertMapper MAPPER = PaymentConvertMapper.INSTANCE;

    @Override
    public void save(ShareReceiver receiver) {
        shareReceiverService.save(MAPPER.toReceiverPO(receiver.getData()));
    }

    @Override
    public ShareReceiver findByAccount(PayChannelEnum channel, String account) {
        ShareReceiverPO po = shareReceiverService.lambdaQuery().eq(ShareReceiverPO::getChannel, channel.name()).eq(ShareReceiverPO::getAccount, account).one();
        return po == null ? null : new ShareReceiver(MAPPER.toReceiverData(po));
    }

    @Override
    public void update(ShareReceiver receiver) {
        ShareReceiverPO po = shareReceiverService.lambdaQuery().eq(ShareReceiverPO::getBizId, receiver.getBizId()).one();
        if (po == null) {
            return;
        }
        ShareReceiverPO update = MAPPER.toReceiverPO(receiver.getData());
        update.setId(po.getId());
        shareReceiverService.updateById(update);
    }

    @Override
    public long countBound(PayChannelEnum channel) {
        return shareReceiverService.lambdaQuery().eq(ShareReceiverPO::getChannel, channel.name()).eq(ShareReceiverPO::getBindStatus, ShareReceiverStatusEnum.BOUND.name()).count();
    }
}
