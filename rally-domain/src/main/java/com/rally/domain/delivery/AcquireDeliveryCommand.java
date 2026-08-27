package com.rally.domain.delivery;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;

/** C2 取得单个业务事件、接收人与渠道组合触达执行权的命令。 */
public record AcquireDeliveryCommand(
        String eventId,
        NotifyBizType bizType,
        String refBizId,
        NoticeScene noticeScene,
        String recipientId,
        NotifyChannel channel) {
}
