package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.meetup.MeetupCardPackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 pack-user-meetup-cards：按当前标签组装“我的约球”卡片页。
 */
@Component
@RequiredArgsConstructor
public class PackUserMeetupCardsActivity {

    private final MeetupCardPackingService packingService;

    public PageDTO<MeetupCardDTO> execute(UserMeetupTabEnum tab,
                                          List<MeetupData> meetups,
                                          boolean hasMore) {
        // A1-A3：映射基础卡片，按标签计算主标签，并补充可降级的球场背景。
        List<MeetupCardDTO> cards = meetups.stream()
                .map(meetup -> packingService.packCardForTab(meetup, tab))
                .toList();

        // A4：总量固定为空；仅在有后续页时以末项约球编号生成游标。
        PageDTO<MeetupCardDTO> page = new PageDTO<>(cards, null, hasMore);
        page.buildCursor(MeetupCardDTO::getMeetupId);
        return page;
    }
}
