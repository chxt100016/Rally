package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送消息返回DTO
 */
@Data
public class ChatSendDTO {

    private String messageId;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
