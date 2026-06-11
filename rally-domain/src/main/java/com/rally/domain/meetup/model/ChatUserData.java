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
    private Integer unreadCount;
    private LocalDateTime joinedAt;
}
