package com.rally.meetup;

import com.rally.domain.meetup.enums.MeetupSortEnum;
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
        PageDTO<MeetupCardDTO> page = new PageDTO<>(res, null, hasMore);
        page.setNextCursor(buildNextCursor(pageData, query.getSort(), hasMore));
        return page;
    }

    /** 生成下一页游标：时间排序用 (startTime, bizId) 复合游标，距离排序用 bizId */
    private String buildNextCursor(List<MeetupData> pageData, MeetupSortEnum sort, boolean hasMore) {
        if (!hasMore || pageData.isEmpty()) {
            return null;
        }
        MeetupData last = pageData.get(pageData.size() - 1);
        PageCursor cursor = sort == MeetupSortEnum.TIME ? PageCursor.ofTime(last.getStartTime(), last.getBizId()) : PageCursor.ofBizId(last.getBizId());
        return PageCursor.encode(cursor);
    }

    /** 生成下一页游标：仅 bizId（用户 Tab 单字段游标） */
    private String buildBizIdCursor(List<MeetupData> pageData, boolean hasMore) {
        if (!hasMore || pageData.isEmpty()) {
            return null;
        }
        return PageCursor.encode(PageCursor.ofBizId(pageData.get(pageData.size() - 1).getBizId()));
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
        PageDTO<MeetupCardDTO> page = new PageDTO<>(cardList, null, pageResult.getHasMore());
        page.setNextCursor(buildBizIdCursor(pageResult.getList(), pageResult.getHasMore()));
        return page;
    }

}
