package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ChatContentTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息领域数据对象
 */
@Data
public class ChatMessageData {

    /** 消息业务主键（雪花ID），同时作为消息拉取的游标 */
    private String bizId;
    private String meetupId;
    private String senderId;
    /** 发送者昵称（冗余存储） */
    private String senderName;
    /** 发送者头像（冗余存储） */
    private String senderAvatar;
    private String content;
    private ChatContentTypeEnum contentType;
    private LocalDateTime createTime;
}
