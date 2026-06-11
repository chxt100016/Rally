package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息DTO
 */
@Data
public class ChatMessageDTO {

    private String messageId;
    private String meetupId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String contentType;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
