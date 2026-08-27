package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-in-progress-meetups：查询当前用户创建或有效参与的开放未结束约球窗口。
 */
@Component
@RequiredArgsConstructor
public class QueryInProgressMeetupsActivity {

    private final UserMeetupQueryDomainService userMeetupQueryDomainService;

    public PageDTO<MeetupData> execute(String userId, String lastId, int limit) {
        // A1-A3：复用既有参与关系、OPEN/未结束筛选与编号游标分页语义。
        return userMeetupQueryDomainService.listInProgress(userId, lastId, limit);
    }
}
