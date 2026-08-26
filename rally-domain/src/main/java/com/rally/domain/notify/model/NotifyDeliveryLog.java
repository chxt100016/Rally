package com.rally.domain.notify.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.enums.NotifyDeliveryStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 触达日志。一条记录对应一个业务事件对一个接收人、一个渠道的一次触达。
 */
@Data
public class NotifyDeliveryLog {

    private Long id;
    private String bizId;
    private String eventId;
    private NotifyBizType bizType;
    private String refBizId;
    private NoticeScene noticeScene;
    private String recipientId;
    private NotifyChannel channel;
    private String providerTemplateId;
    private NotifyDeliveryStatus status;
    private String providerMessageId;
    private String errorCode;
    private String failReason;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static NotifyDeliveryLog sending(String eventId, NotifyBizType bizType, String refBizId,
                                            NoticeScene scene, String recipientId, NotifyChannel channel) {
        NotifyDeliveryLog log = new NotifyDeliveryLog();
        log.setBizId(IdWorker.getIdStr());
        log.setEventId(eventId);
        log.setBizType(bizType);
        log.setRefBizId(refBizId);
        log.setNoticeScene(scene);
        log.setRecipientId(recipientId);
        log.setChannel(channel);
        log.setStatus(NotifyDeliveryStatus.SENDING);
        return log;
    }
}
