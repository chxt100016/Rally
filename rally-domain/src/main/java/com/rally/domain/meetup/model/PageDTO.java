package com.rally.domain.meetup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    /** 数据列表 */
    private List<T> list;
    /** 总数 */
    private Long total;
    /** 是否有更多 */
    private Boolean hasMore;
}
