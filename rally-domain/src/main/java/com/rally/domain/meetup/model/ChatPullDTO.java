package com.rally.domain.meetup.model;

import lombok.Data;

import java.util.List;

/**
 * 拉取消息返回DTO
 */
@Data
public class ChatPullDTO {

    private List<ChatMessageDTO> messages;
    /** 下次拉取的游标（消息bizId），前端原样回传；清空缓存后不传则从头拉取历史 */
    private String lastMessageId;
}
