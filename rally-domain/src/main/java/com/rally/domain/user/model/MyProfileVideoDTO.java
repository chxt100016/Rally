package com.rally.domain.user.model;

import lombok.Data;

import java.util.List;

/**
 * 我的档案 - 视频信息
 */
@Data
public class MyProfileVideoDTO {

    /** 视频总数 */
    private Integer total;

    /** 视频列表 */
    private List<VideoItemDTO> data;
}
