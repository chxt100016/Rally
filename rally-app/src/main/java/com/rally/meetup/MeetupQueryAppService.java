package com.rally.meetup;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        return switch (query.getSort()) {
            case DISTANCE -> meetupQueryDomainService.listByDistance(query);
            case TIME -> meetupQueryDomainService.listByTime(query);
            default -> null;
        };
    }

    /**
     * 用户约球列表查询（按 Tab 筛选：待处理/进行中/我发布/已完成）
     */
    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        return meetupQueryDomainService.listByUser(cmd, userId);
    }

}
