package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-recent-meetups：查询当前用户创建或有效参与的全部非草稿约球窗口。
 */
@Component
@RequiredArgsConstructor
public class QueryRecentMeetupsActivity {

    private final UserMeetupQueryDomainService userMeetupQueryDomainService;

    public PageDTO<MeetupData> execute(String userId, String lastId, int limit) {
        // A1-A3：复用既有创建者/有效参与、非草稿筛选、编号游标与 limit 窗口语义。
        return userMeetupQueryDomainService.listRecent(userId, lastId, limit);
    }
}
