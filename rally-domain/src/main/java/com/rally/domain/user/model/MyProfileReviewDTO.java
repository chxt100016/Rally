package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 我的档案 - 评价信息
 */
@Data
@Accessors(chain = true)
public class MyProfileReviewDTO {

    /** 评价总数 */
    private Integer total;

    /** 评价标签 */
    private List<ReviewTagDTO> tags;
}
