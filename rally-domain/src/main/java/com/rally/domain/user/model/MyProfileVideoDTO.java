package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 我的档案 - 视频信息
 */
@Data
@Accessors(chain = true)
public class MyProfileVideoDTO {

    /** 视频总数 */
    private Integer total;

    private Integer maxCount;

    private Integer maxSizeMb;

    private Integer maxSecond;

    /** 视频列表 */
    private List<VideoItemDTO> data;
}
