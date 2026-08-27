package com.rally.meetup;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListCmd;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.meetup.activity.PackMeetupSquareCardsActivity;
import com.rally.meetup.activity.SearchAvailableMeetupsByDistanceActivity;
import com.rally.meetup.activity.SearchAvailableMeetupsByTimeActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球查询应用服务
 * 编排领域服务完成查询场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupQueryAppService {

    private final SearchAvailableMeetupsByDistanceActivity searchAvailableMeetupsByDistanceActivity;
    private final SearchAvailableMeetupsByTimeActivity searchAvailableMeetupsByTimeActivity;
    private final PackMeetupSquareCardsActivity packMeetupSquareCardsActivity;

    /**
     * 约球列表查询（按时间/距离）
     */
    public PageDTO<MeetupCardDTO> queryMeetupList(MeetupListCmd query) {
        List<Object> cursor = PageDTO.parseCursor(query.getLastId());
        query.setLastBizId(cursor.isEmpty() ? null : (String) cursor.get(0));
        query.setLastStartTime(cursor.size() > 1 ? LocalDateTime.parse(cursor.get(1).toString()) : null);
        List<MeetupData> dataList = switch (query.getSort()) {
            case DISTANCE -> searchAvailableMeetupsByDistanceActivity.execute(query);
            case TIME -> searchAvailableMeetupsByTimeActivity.execute(query);
            default -> List.of();
        };
        return packMeetupSquareCardsActivity.execute(
                dataList,
                query.getPageSize(),
                query.getSort(),
                query.getLng(),
                query.getLat());
    }

}
