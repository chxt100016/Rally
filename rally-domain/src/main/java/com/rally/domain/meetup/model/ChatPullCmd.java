package com.rally.domain.meetup.model;

import lombok.Data;

/**
 * 拉取消息命令
 */
@Data
public class ChatPullCmd {

    private String meetupId;
    /** 上次拉取返回的游标（消息bizId），不传则从头拉取历史消息 */
    private String lastMessageId;
    private Integer limit;
}
