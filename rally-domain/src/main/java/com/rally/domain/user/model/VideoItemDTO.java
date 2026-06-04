package com.rally.domain.user.model;

import lombok.Data;

/**
 * 视频项
 */
@Data
public class VideoItemDTO {

    /** 视频存储key */
    private String key;

    /** 视频访问URL */
    private String url;
}
