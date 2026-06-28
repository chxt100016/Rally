package com.rally.domain.payment.service;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.ShareReceiverRepository;
import com.rally.domain.payment.model.ShareReceiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 分账接收方账本领域服务（见设计 §15.5、§5.7）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareReceiverDomainService {

    private final ShareReceiverRepository shareReceiverRepository;
    private final PaymentChannelRouter channelRouter;

    /**
     * 幂等绑定（发起收款前置，§5.1-4）：账本无 BOUND → 渠道 add + bind()；已绑定 → touch。微信"已存在"由 client 视为成功。
     */
    public ShareReceiver ensureBound(String userId, String openid, PayChannelEnum channel) {
        PaymentChannelClient client = channelRouter.route(channel);
        ShareReceiver receiver = shareReceiverRepository.findByAccount(channel, openid);
        if (receiver == null) {
            receiver = ShareReceiver.create(channel, userId, openid);
            client.addShareReceiver(receiver);
            receiver.bind();
            shareReceiverRepository.save(receiver);
            return receiver;
        }
        if (receiver.isBound()) {
            receiver.touch(LocalDateTime.now());
        } else {
            client.addShareReceiver(receiver);
            receiver.bind();
        }
        shareReceiverRepository.update(receiver);
        return receiver;
    }

    /**
     * 淘汰（非 MVP，§5.7）：调用方已校验无 PROCESSING 分账 → 渠道 delete + unbind()。
     */
    public void evict(ShareReceiver receiver) {
        channelRouter.route(receiver.getData().getChannel()).deleteShareReceiver(receiver);
        receiver.unbind();
        shareReceiverRepository.update(receiver);
    }
}
