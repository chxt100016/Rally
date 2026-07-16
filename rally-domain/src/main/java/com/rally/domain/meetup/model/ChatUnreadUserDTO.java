package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未读用户DTO
 */
@Data
public class ChatUnreadUserDTO {

    private String userId;
    private String nickname;
    private String avatarUrl;

    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReadTime;
}
