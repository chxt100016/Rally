package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
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
    private final MeetupCardPackingService packingService;

    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        int limit = cmd.getSize() + 1;
        List<Object> cursor = PageDTO.parseCursor(cmd.getLastId());
        String lastId = cursor.isEmpty() ? null : (String) cursor.get(0);
        PageDTO<MeetupData> pageResult = switch (cmd.getTab()) {
            case PENDING -> userMeetupQueryDomainService.listPending(userId, lastId, limit);
            case IN_PROGRESS -> userMeetupQueryDomainService.listInProgress(userId, lastId, limit);
            case MY_PUBLISH -> userMeetupQueryDomainService.listMyPublish(userId, lastId, limit);
            case COMPLETED -> userMeetupQueryDomainService.listCompleted(userId, lastId, limit);
            case RECENT -> userMeetupQueryDomainService.listRecent(userId, lastId, limit);
        };
        List<MeetupCardDTO> cardList = pageResult.getList().stream()
                .map(data -> packingService.packCardForTab(data, cmd.getTab()))
                .toList();

        PageDTO<MeetupCardDTO> page = new PageDTO<>(cardList, null, pageResult.getHasMore());
        page.buildCursor(MeetupCardDTO::getMeetupId);
        return page;
    }
}
