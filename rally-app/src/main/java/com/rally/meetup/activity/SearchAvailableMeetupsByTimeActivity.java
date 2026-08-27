package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 search-available-meetups-by-time：按开球时间与复合游标查询可用约球窗口。
 */
@Component
@RequiredArgsConstructor
public class SearchAvailableMeetupsByTimeActivity {

    private final MeetupQueryDomainService meetupQueryDomainService;

    public List<MeetupData> execute(MeetupListCmd query) {
        // A1-A3：复用既有查询规划与仓储实现，保持筛选、复合游标及 pageSize+1 语义不变。
        return meetupQueryDomainService.listByTime(query);
    }
}
