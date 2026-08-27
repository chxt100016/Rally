package com.rally.personalprofile.playerhome.activity;

import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import com.rally.domain.user.model.PlayerHomeMeetupDTO;
import com.rally.meetup.MeetupCardPackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 query-player-meetup-summary：汇总目标球员的完成与最近约球。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerMeetupSummaryActivity {

    private static final int RECENT_QUERY_LIMIT = 4;

    private final UserMeetupQueryDomainService userMeetupQueryDomainService;
    private final MeetupCardPackingService meetupCardPackingService;

    public PlayerHomeMeetupDTO execute(String userId) {
        // A1：仅统计 REVIEWED/SKIPPED 报名对应的非草稿约球。
        long completedCount = userMeetupQueryDomainService.countCompleted(userId);

        // A2：发布者或有效参与者的非草稿约球，按 bizId 倒序多取一条。
        List<MeetupData> recentMeetups = userMeetupQueryDomainService
                .listRecent(userId, null, RECENT_QUERY_LIMIT)
                .getList();

        // A3：沿用 RECENT 卡片语义，保留状态文案、场地空值与背景降级行为。
        List<MeetupCardDTO> cards = recentMeetups.stream()
                .map(meetup -> meetupCardPackingService.packCardForTab(meetup, UserMeetupTabEnum.RECENT))
                .toList();

        return new PlayerHomeMeetupDTO()
                .setCompletedCount((int) completedCount)
                .setRecentMeetups(cards);
    }
}
