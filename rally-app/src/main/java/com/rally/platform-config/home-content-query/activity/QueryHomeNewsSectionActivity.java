package com.rally.platformconfig.homecontentquery.activity;

import com.alibaba.fastjson2.JSONObject;
import com.rally.home.model.DisplayType;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.NewsTimelineDisplayData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 业务活动 query-home-news-section：组装首页资讯区块。
 */
@Component
public class QueryHomeNewsSectionActivity {

    public HomeDisplayItemDTO execute(JSONObject section) {
        // A1：保留配置的非空文案，空白时回退固定默认值。
        NewsTimelineDisplayData data = new NewsTimelineDisplayData();
        data.setTitle(configuredText(section, "title", "资讯"));
        data.setSubtitle(configuredText(section, "subtitle", "最新动态"));

        // A2：当前资讯数据源固定为空，不查询或填充新闻条目。
        data.setNewsItems(new ArrayList<>());

        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.NEWS_TIMELINE);
        item.setData(data);
        return item;
    }

    private String configuredText(JSONObject section, String key, String fallback) {
        String value = section.getString(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
