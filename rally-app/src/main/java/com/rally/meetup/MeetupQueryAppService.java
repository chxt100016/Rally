package com.rally.meetup;

import com.rally.domain.meetup.enums.MeetupSortEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListCmd;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 约球查询应用服务
 * 编排领域服务完成查询场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupQueryAppService {

    private final MeetupQueryDomainService meetupQueryDomainService;
    private final MeetupCardPackingService packingService;

    /**
     * 约球列表查询（按时间/距离）
     */
    public PageDTO<MeetupCardDTO> queryMeetupList(MeetupListCmd query) {
        List<MeetupData> dataList = switch (query.getSort()) {
            case DISTANCE -> meetupQueryDomainService.listByDistance(query);
            case TIME -> meetupQueryDomainService.listByTime(query);
            default -> List.of();
        };
        boolean hasMore = dataList.size() > query.getPageSize();
        List<MeetupData> pageData = hasMore ? dataList.subList(0, query.getPageSize()) : dataList;
        List<MeetupCardDTO> res = pageData.stream().map(item -> packingService.packCard(item, query.getLng(), query.getLat())).toList();
        PageDTO<MeetupCardDTO> page = new PageDTO<>(res, null, hasMore);
        if (!res.isEmpty()) {
            MeetupCardDTO last = res.get(res.size() - 1);
            page.buildCursor(query.getSort() == MeetupSortEnum.TIME
                    ? Map.of("startTime", last.getStartTime().toString(), "bizId", last.getMeetupId())
                    : Map.of("bizId", last.getMeetupId()));
        }
        return page;
    }

}
