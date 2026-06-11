package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ChatContentTypeEnum;
import lombok.Data;

/**
 * 发送消息命令
 */
@Data
public class ChatSendCmd {

    private String meetupId;
    private String content;
    private ChatContentTypeEnum contentType;
}
