package com.rally.domain.notify.gateway;

import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.model.NotifyMessage;
import com.rally.domain.notify.model.NotifyResult;

/**
 * 通知渠道抽象。微信订阅消息为其一种实现（在 infrastructure 层）。
 */
public interface Notifier {

    /** 所属渠道 */
    NotifyChannel channel();

    /** 发送一条通知 */
    NotifyResult send(NotifyMessage message);
}
