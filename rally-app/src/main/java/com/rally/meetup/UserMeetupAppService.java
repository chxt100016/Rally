package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMeetupAppService {

    private final MeetupQueryDomainService meetupQueryDomainService;
    private final MeetupCardPackingService packingService;

    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        int limit = cmd.getSize() + 1;
        List<Object> cursor = PageDTO.parseCursor(cmd.getLastId());
        String lastId = cursor.isEmpty() ? null : (String) cursor.get(0);
        PageDTO<MeetupData> pageResult = switch (cmd.getTab()) {
            case PENDING -> meetupQueryDomainService.listPending(userId, lastId, limit);
            case IN_PROGRESS -> meetupQueryDomainService.listInProgress(userId, lastId, limit);
            case MY_PUBLISH -> meetupQueryDomainService.listMyPublish(userId, lastId, limit);
            case COMPLETED -> meetupQueryDomainService.listCompleted(userId, lastId, limit);
            case RECENT -> meetupQueryDomainService.listRecent(userId, lastId, limit);
        };
        List<MeetupCardDTO> cardList = pageResult.getList().stream()
                .map(data -> packingService.packCardForTab(data, cmd.getTab()))
                .toList();

        PageDTO<MeetupCardDTO> page = new PageDTO<>(cardList, null, pageResult.getHasMore());
        page.buildCursor(MeetupCardDTO::getMeetupId);
        return page;
    }
}
