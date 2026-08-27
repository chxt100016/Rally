package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-pending-meetups：查询当前用户的待审批、待评价与未读约球窗口。
 */
@Component
@RequiredArgsConstructor
public class QueryPendingMeetupsActivity {

    private final UserMeetupQueryDomainService userMeetupQueryDomainService;

    public PageDTO<MeetupData> execute(String userId, String lastId, int limit) {
        // A1-A5：复用既有配置降级与 UNION 查询，保留原因重复、编号游标和 limit 窗口语义。
        return userMeetupQueryDomainService.listPending(userId, lastId, limit);
    }
}
