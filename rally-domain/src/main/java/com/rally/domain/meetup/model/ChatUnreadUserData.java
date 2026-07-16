package com.rally.domain.meetup.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未读用户领域数据对象
 */
@Data
@AllArgsConstructor
public class ChatUnreadUserData {

    private String userId;
    /** 从未拉取过消息时为 null */
    private LocalDateTime lastReadTime;
}
