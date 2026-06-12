package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.ChatContentTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息命令
 */
@Data
public class ChatSendCmd {

    @NotBlank(message = "meetupId不能为空")
    private String meetupId;

    @NotBlank(message = "content不能为空")
    private String content;

    @NotNull(message = "contentType不能为空")
    private ChatContentTypeEnum contentType;
}
