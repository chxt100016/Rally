package com.rally.domain.user.model;

import lombok.Data;

/**
 * 七牛直传凭证响应
 */
@Data
public class VideoTokenVO {
    private String uploadToken;
    /** 前缀模式（视频多文件） */
    private String keyPrefix;
    /** 固定 key 模式（头像覆盖） */
    private String key;
    private int maxSizeMb;
    private int maxDurationSec;
    private String uploadHost;
    private String resourceUrl;
}
