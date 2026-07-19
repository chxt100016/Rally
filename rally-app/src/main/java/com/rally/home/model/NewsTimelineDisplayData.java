package com.rally.home.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class NewsTimelineDisplayData extends BaseDisplayData {
    private List<NewsItem> newsItems;

    @Data
    public static class NewsItem {
        private String newsId;
        private String title;
        private String summary;
        @JSONField(format = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishTime;
        private String coverImage;
        private String linkUrl;
    }
}
