package com.rally.meetup;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
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
        // 数据库数据转 DTO 在应用层完成
        List<MeetupCardDTO> cardList = MeetupAppConvertMapper.INSTANCE.toMeetupCardDTOList(pageData);
        return new PageDTO<>(cardList, null, hasMore);
    }

    /**
     * 用户约球列表查询（按 Tab 筛选：待处理/进行中/我发布/已完成）
     */
    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        return meetupQueryDomainService.listByUser(cmd, userId);
    }

}
