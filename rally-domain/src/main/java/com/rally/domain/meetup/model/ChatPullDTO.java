package com.rally.domain.meetup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 拉取消息返回DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPullDTO {

    private List<ChatMessageDTO> messages;

    /** 未读最新消息的用户列表，仅 withUnreadUsers=true 时返回 */
    private List<ChatUnreadUserDTO> unreadUsers;

}
