package com.rally.domain.payment.gateway;

import com.rally.domain.payment.model.PaymentOrder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单读写网关（操作中间件 → Repository 后缀）。
 * 状态推进一律条件更新（where status=期望值）防并发覆盖。
 */
public interface PaymentOrderRepository {

    /** 批量新增（发起收款，uk_batch_payer 防重） */
    void saveBatch(List<PaymentOrder> orders);

    /** 按 bizId（out_trade_no）查询 */
    PaymentOrder findByBizId(String bizId);

    /** 本场全部支付单（详情页收款进度） */
    List<PaymentOrder> listByMeetup(String meetupId);

    /** 多活动批量（列表页 IN，避免 N+1） */
    List<PaymentOrder> listByMeetups(List<String> meetupIds);

    /** 本场待支付支付单（关闭收款） */
    List<PaymentOrder> listPendingByMeetup(String meetupId);

    /** 某用户的待支付支付单（待处理列表「我有待支付」） */
    List<PaymentOrder> listPendingByPayer(String payerUserId);

    /** 扫描超时未支付订单（仅在配置了超时时使用：status=PENDING && expire_time IS NOT NULL && expire_time<now） */
    List<PaymentOrder> listExpiredPending(LocalDateTime now);

    /** 本场是否已存在任意支付单（发起收款幂等校验） */
    boolean existsByMeetup(String meetupId);

    /** 条件更新为已支付（where status=PENDING），返回是否更新成功 */
    boolean markPaid(String bizId, String transactionId, LocalDateTime payTime);

    /** 条件更新为已关闭（where status=PENDING） */
    boolean close(String bizId);

    /** 条件更新为失败（where status=PENDING） */
    boolean markFailed(String bizId, String reason);
}
