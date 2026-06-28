package com.rally.domain.payment.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道路由（见设计 §4.2）：Spring 注入全部 {@link PaymentChannelClient} 实现，按 {@code channel()} 建 map。
 */
@Component
@RequiredArgsConstructor
public class PaymentChannelRouter {

    private final List<PaymentChannelClient> clients;
    private final Map<PayChannelEnum, PaymentChannelClient> clientMap = new EnumMap<>(PayChannelEnum.class);

    @PostConstruct
    public void init() {
        for (PaymentChannelClient client : clients) {
            clientMap.put(client.channel(), client);
        }
    }

    /** 路由到对应渠道实现，缺失抛 PAYMENT_CHANNEL_NOT_SUPPORTED */
    public PaymentChannelClient route(PayChannelEnum channel) {
        PaymentChannelClient client = clientMap.get(channel);
        if (client == null) {
            throw new BusinessException(BizErrorCode.PAYMENT_CHANNEL_NOT_SUPPORTED);
        }
        return client;
    }
}
