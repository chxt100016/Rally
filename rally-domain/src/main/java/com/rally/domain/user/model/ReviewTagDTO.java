package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 我的档案 - 评价标签
 */
@Data
@Accessors(chain = true)
public class ReviewTagDTO {

    /** 标签名称 */
    private String name;
}
