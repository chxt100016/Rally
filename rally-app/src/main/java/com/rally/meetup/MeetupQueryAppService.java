package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
        // searchAfter：多查了 1 条用于判断是否还有下一页，命中则去掉多余的一条
        boolean hasMore = dataList.size() > query.getPageSize();
        List<MeetupData> pageData = hasMore ? dataList.subList(0, query.getPageSize()) : dataList;

        List<MeetupCardDTO> res = pageData.stream().map(item -> packingService.packCard(item, query.getLng(), query.getLat())).toList();
        return new PageDTO<>(res, null, hasMore);
    }

    /**
     * 用户约球列表查询（按 Tab 筛选：待处理/进行中/我发布/已完成）
     */
    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        PageDTO<MeetupData> pageResult = meetupQueryDomainService.listByUser(cmd, userId);
        List<MeetupCardDTO> cardList = pageResult.getList().stream()
                .map(data -> packingService.packCardForTab(data, cmd.getTab()))
                .toList();
        return new PageDTO<>(cardList, null, pageResult.getHasMore());
    }

}
