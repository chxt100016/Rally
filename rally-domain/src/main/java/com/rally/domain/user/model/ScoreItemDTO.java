package com.rally.domain.user.model;

import lombok.Data;

/**
 * 评分明细项
 */
@Data
public class ScoreItemDTO {

    /** 评分名称 */
    private String name;

    /** 评分key */
    private String key;

    /** 评分值 */
    private String value;

    /** 权重标签 */
    private String label;

    /** 说明信息 */
    private String info;

    /** 排序 */
    private String sort;
}
