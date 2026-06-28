package com.rally.domain.payment.gateway;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.model.ShareReceiver;

/**
 * 分账接收方账本读写网关（uk_channel_account 幂等 upsert）。
 */
public interface ShareReceiverRepository {

    /** 新增接收方 */
    void save(ShareReceiver receiver);

    /** 按渠道 + 接收方标识查询 */
    ShareReceiver findByAccount(PayChannelEnum channel, String account);

    /** 更新接收方（bind/touch/unbind 后持久化） */
    void update(ShareReceiver receiver);

    /** 统计某渠道已绑定接收方数量（接近上限触发 LRU 淘汰） */
    long countBound(PayChannelEnum channel);
}
