package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动群聊用户PO
 */
@Data
@TableName("rally_meetup_chat_user")
public class ChatUserPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String meetupId;
    private String userId;
    private String lastReadMessageId;
    private LocalDateTime lastReadTime;
    private Integer unreadCount;
    private LocalDateTime joinedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
