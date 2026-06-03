package com.rally.domain.user.model;

import lombok.Data;

/**
 * 七牛 callback 入参
 */
@Data
public class VideoCallbackCmd {
    /** 七牛对象 key，须以 videos/{user_id}/ 开头 */
    private String key;
    /** 用户 ID */
    private String userId;
    /** 文件大小 */
    private Long fsize;
    /** 时长（秒） */
    private Float duration;
}
