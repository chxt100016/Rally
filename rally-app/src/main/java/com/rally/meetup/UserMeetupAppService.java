package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import com.rally.meetup.activity.QueryCompletedMeetupsActivity;
import com.rally.meetup.activity.QueryInProgressMeetupsActivity;
import com.rally.meetup.activity.PackUserMeetupCardsActivity;
import com.rally.meetup.activity.QueryPendingMeetupsActivity;
import com.rally.meetup.activity.QueryPublishedMeetupsActivity;
import com.rally.meetup.activity.QueryRecentMeetupsActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMeetupAppService {

    private final UserMeetupQueryDomainService userMeetupQueryDomainService;
    private final QueryPendingMeetupsActivity queryPendingMeetupsActivity;
    private final QueryInProgressMeetupsActivity queryInProgressMeetupsActivity;
    private final QueryPublishedMeetupsActivity queryPublishedMeetupsActivity;
    private final QueryCompletedMeetupsActivity queryCompletedMeetupsActivity;
    private final QueryRecentMeetupsActivity queryRecentMeetupsActivity;
    private final PackUserMeetupCardsActivity packUserMeetupCardsActivity;

    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        int limit = cmd.getSize() + 1;
        List<Object> cursor = PageDTO.parseCursor(cmd.getLastId());
        String lastId = cursor.isEmpty() ? null : (String) cursor.get(0);
        PageDTO<MeetupData> pageResult = switch (cmd.getTab()) {
            case PENDING -> queryPendingMeetupsActivity.execute(userId, lastId, limit);
            case IN_PROGRESS -> queryInProgressMeetupsActivity.execute(userId, lastId, limit);
            case MY_PUBLISH -> queryPublishedMeetupsActivity.execute(userId, lastId, limit);
            case COMPLETED -> queryCompletedMeetupsActivity.execute(userId, lastId, limit);
            case RECENT -> queryRecentMeetupsActivity.execute(userId, lastId, limit);
        };
        return packUserMeetupCardsActivity.execute(cmd.getTab(), pageResult.getList(), pageResult.getHasMore());
    }
}
