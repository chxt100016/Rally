package com.rally.platformconfig.homecontentquery.activity;

import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.home.model.DisplayType;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.MeetupDisplayData;
import com.rally.meetup.UserMeetupAppService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务活动 query-home-meetup-section：组装首页“我的约球”区块。
 */
@Component
@RequiredArgsConstructor
public class QueryHomeMeetupSectionActivity {

    private final UserMeetupAppService userMeetupAppService;

    public HomeDisplayItemDTO execute(JSONObject section) {
        // A1：标题空白时回退固定文案，副标题保留配置原值。
        MeetupDisplayData data = new MeetupDisplayData();
        data.setTitle(configuredText(section, "title", "我的约球"));
        data.setSubtitle(section.getString("subtitle"));

        // A2/A3：匿名不查库；登录用户复用进行中 Tab 的筛选、排序、默认 10 条与卡片映射语义。
        data.setMeetups(queryInProgressMeetups());

        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.MEETUP);
        item.setData(data);
        return item;
    }

    private List<MeetupCardDTO> queryInProgressMeetups() {
        String userId = UserContext.getIfPresent();
        if (userId == null) {
            return new ArrayList<>();
        }

        UserMeetupListCmd command = new UserMeetupListCmd();
        command.setTab(UserMeetupTabEnum.IN_PROGRESS);
        PageDTO<MeetupCardDTO> page = userMeetupAppService.queryUserMeetupList(command);
        return page.getList();
    }

    private String configuredText(JSONObject section, String key, String fallback) {
        String value = section.getString(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
