package com.rally.domain.behavior.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UserBehaviorLogData {

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
}
