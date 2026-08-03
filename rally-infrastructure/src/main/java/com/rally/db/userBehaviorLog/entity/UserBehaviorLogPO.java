package com.rally.db.userBehaviorLog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_behavior_log")
public class UserBehaviorLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String requestId;
    private String httpMethod;
    private String requestUri;
    private String routePattern;
    private String requestParams;
    private Boolean paramsTruncated;
    private String clientIp;
    private String userAgent;
    private Integer httpStatus;
    private Long durationMs;
    private String exceptionType;
    private LocalDateTime occurredAt;
    private LocalDateTime createTime;
}
