package com.rally.domain.payment.service;

import com.rally.domain.payment.enums.CollectionStateEnum;
import com.rally.domain.payment.enums.PaymentViewStatus;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付读模型领域服务（见设计 §5.5/§5.6，约球域零改动）。
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryDomainService {

    private final PaymentOrderRepository paymentOrderRepository;

    /** payerUserId -> 视图态，详情页合并 */
    public Map<String, PaymentViewStatus> statusByMeetup(String meetupId) {
        return toViewMap(paymentOrderRepository.listByMeetup(meetupId));
    }

    /** 列表页 IN 批量，避免 N+1。返回 meetupId -> (payerUserId -> 视图态) */
    public Map<String, Map<String, PaymentViewStatus>> statusByMeetups(List<String> meetupIds) {
        Map<String, Map<String, PaymentViewStatus>> result = new LinkedHashMap<>();
        if (meetupIds == null || meetupIds.isEmpty()) {
            return result;
        }
        for (PaymentOrder order : paymentOrderRepository.listByMeetups(meetupIds)) {
            result.computeIfAbsent(order.getData().getMeetupId(), k -> new LinkedHashMap<>())
                    .put(order.getData().getPayerUserId(), order.toViewStatus());
        }
        return result;
    }

    /** 待处理列表"我有待支付" */
    public List<PaymentOrder> myPending(String userId) {
        return paymentOrderRepository.listPendingByPayer(userId);
    }

    /** 详情页按钮态：无单→「发起收款」 */
    public boolean hasAnyOrder(String meetupId) {
        return paymentOrderRepository.existsByMeetup(meetupId);
    }

    /** 有 PENDING→「关闭收款」 */
    public boolean hasPending(String meetupId) {
        return !paymentOrderRepository.listPendingByMeetup(meetupId).isEmpty();
    }

    /** 发起人收款入口态：无单→INITIABLE / 有 PENDING→ONGOING / 有单无 PENDING→ENDED */
    public CollectionStateEnum collectionState(String meetupId) {
        List<PaymentOrder> orders = paymentOrderRepository.listByMeetup(meetupId);
        if (orders.isEmpty()) {
            return CollectionStateEnum.INITIABLE;
        }
        boolean anyPending = orders.stream().anyMatch(PaymentOrder::isPending);
        return anyPending ? CollectionStateEnum.ONGOING : CollectionStateEnum.ENDED;
    }

    /** 当前用户本场支付态；非应付人返回 null */
    public PaymentViewStatus myPaymentStatus(String meetupId, String userId) {
        return paymentOrderRepository.listByMeetup(meetupId).stream()
                .filter(o -> o.getData().getPayerUserId().equals(userId))
                .map(PaymentOrder::toViewStatus)
                .findFirst().orElse(null);
    }

    private Map<String, PaymentViewStatus> toViewMap(List<PaymentOrder> orders) {
        Map<String, PaymentViewStatus> map = new LinkedHashMap<>();
        for (PaymentOrder order : orders) {
            map.put(order.getData().getPayerUserId(), order.toViewStatus());
        }
        return map;
    }
}
