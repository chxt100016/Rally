package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 视频项
 */
@Data
@Accessors(chain = true)
public class VideoItemDTO {

    /** 视频存储key */
    private String key;

    /** 视频访问URL */
    private String url;

    private String coverUrl;

    private String image;

    private String title;
}
