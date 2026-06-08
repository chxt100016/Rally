package com.rally.domain.recap.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 对外评价视图（球员主页三区分组）
 */
@Data
public class ReviewVO {
    /** 被评价人 user_id */
    private String toUserId;
    /** NTRP 投票统计：higher/same/lower 各多少票 */
    private Map<String, Long> ntrpVote;
    /** 出勤统计：on_time/late/no_show 各多少次 */
    private Map<String, Long> attendance;
    /** 标签列表（带次数，按 count 降序） */
    private List<TagCount> tags;

    /**
     * 标签计数
     */
    @Data
    public static class TagCount {
        private String name;
        private Long count;

        public TagCount(String name, Long count) {
            this.name = name;
            this.count = count;
        }
    }
}
