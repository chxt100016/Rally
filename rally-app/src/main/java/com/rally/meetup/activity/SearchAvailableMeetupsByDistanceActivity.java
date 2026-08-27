package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 search-available-meetups-by-distance：按球面距离排序全量候选并用约球编号截取窗口。
 */
@Component
@RequiredArgsConstructor
public class SearchAvailableMeetupsByDistanceActivity {

    private final MeetupQueryDomainService meetupQueryDomainService;

    public List<MeetupData> execute(MeetupListCmd query) {
        // A1-A3：复用既有查询规划、距离排序与内存游标截取逻辑，保持公共契约不变。
        return meetupQueryDomainService.listByDistance(query);
    }
}
