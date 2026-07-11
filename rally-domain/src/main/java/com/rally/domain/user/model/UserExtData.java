package com.rally.domain.user.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserExtData {
    private String bizId;
    private String userId;
    private String extKey;
    private String extValue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
