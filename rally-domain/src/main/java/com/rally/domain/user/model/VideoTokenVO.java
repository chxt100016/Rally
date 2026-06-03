package com.rally.domain.user.model;

import lombok.Data;

/**
 * 七牛直传凭证响应
 */
@Data
public class VideoTokenVO {
    private String uploadToken;
    private String keyPrefix;
    private int maxSizeMb;
    private int maxDurationSec;
    private String uploadHost;
}
