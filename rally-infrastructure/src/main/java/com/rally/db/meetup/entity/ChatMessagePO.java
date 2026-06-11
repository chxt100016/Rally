package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动群聊消息PO
 */
@Data
@TableName("rally_meetup_chat_message")
public class ChatMessagePO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String meetupId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String contentType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
