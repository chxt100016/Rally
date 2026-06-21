package com.rally.domain.notify.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifySubscribeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅通知流水（一条 = 一次真实的微信订阅额度）
 */
@Data
public class NotifySubscribe {

    /** 自增主键（内部用，CAS/回写定位） */
    private Long id;
    /** 雪花流水ID（流程ID） */
    private String bizId;
    /** 接收人（被通知用户） */
    private String userId;
    /** 业务方向 */
    private NotifyBizType bizType;
    /** 关联业务对象ID（当前为 meetupId） */
    private String refBizId;
    /** 通知场景 */
    private NoticeScene noticeScene;
    /** 微信订阅模板ID（冗余，便于排查） */
    private String templateId;
    /** 状态 */
    private NotifySubscribeStatus status;
    /** 失败原因 */
    private String failReason;
    /** 发送时间 */
    private LocalDateTime sendTime;
    /** 订阅额度过期时间 */
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 创建一条已授权待发流水
     */
    public static NotifySubscribe granted(String userId, NotifyBizType bizType, String refBizId, NoticeScene scene) {
        NotifySubscribe subscribe = new NotifySubscribe();
        subscribe.setBizId(IdWorker.getIdStr());
        subscribe.setUserId(userId);
        subscribe.setBizType(bizType);
        subscribe.setRefBizId(refBizId);
        subscribe.setNoticeScene(scene);
        subscribe.setTemplateId(scene.getTemplateId());
        subscribe.setStatus(NotifySubscribeStatus.UNUSED);
        return subscribe;
    }
}
