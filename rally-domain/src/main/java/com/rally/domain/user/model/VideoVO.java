package com.rally.domain.user.model;

import lombok.Data;

/**
 * 视频信息值对象
 */
@Data
public class VideoVO {
    /**
     * 视频 key（存储标识）
     */
    private String key;

    /**
     * 视频标题
     */
    private String title;
}
