package com.rally.domain.review.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可评价人列表 VO
 */
@Data
public class ReviewableListVO {
    /** 是否已超期（前端置灰提交按钮） */
    private Boolean deadlinePassed;
    /** 截止时间 */
    private LocalDateTime deadlineAt;
    /** 可评价人列表 */
    private List<ReviewablePersonVO> people;
}
