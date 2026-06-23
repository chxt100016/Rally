package com.rally.domain.meetup.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页视图
 */
@Data
@NoArgsConstructor
public class PageDTO<T> {
    /** 数据列表 */
    private List<T> list;
    /** 总数 */
    private Long total;
    /** 是否有更多 */
    private Boolean hasMore;
    /** 下一页游标（searchAfter）：hasMore 为 true 时返回，前端原样回传作为下一页 lastId；无更多时为 null */
    private String nextCursor;

    public PageDTO(List<T> list, Long total, Boolean hasMore) {
        this.list = list;
        this.total = total;
        this.hasMore = hasMore;
    }
}
