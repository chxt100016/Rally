package com.rally.domain.meetup.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天用户领域数据对象
 */
@Data
public class ChatUserData {

    private String bizId;
    private String meetupId;
    private String userId;
    /** 已读最新消息bizId，仅用于未读数计算，不参与拉取游标 */
    private String lastReadMessageId;
    /** 最后一次已读时间，仅真实拉取触发已读时更新 */
    private LocalDateTime lastReadTime;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
}
