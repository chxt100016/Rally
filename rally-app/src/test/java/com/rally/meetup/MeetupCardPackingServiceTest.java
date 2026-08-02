package com.rally.meetup;

import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

public class MeetupCardPackingServiceTest {

    private final MeetupCardPackingService packingService = new MeetupCardPackingService(null);

    @Test
    public void shouldSetBackgroundKeyForMeetupListCard() {
        MeetupCardDTO card = packingService.packCard(meetupData(), null, null);

        assertEquals("hard-day-clear", card.getBackgroundKey());
    }

    @Test
    public void shouldSetBackgroundKeyForUserMeetupCard() {
        MeetupCardDTO card = packingService.packCardForTab(meetupData(), UserMeetupTabEnum.RECENT);

        assertEquals("hard-day-clear", card.getBackgroundKey());
    }

    private MeetupData meetupData() {
        MeetupData data = new MeetupData();
        data.setBizId("meetup-1");
        data.setStatus(MeetupStatusEnum.OPEN);
        data.setDistrictName("浦东新区");
        data.setStartTime(LocalDateTime.of(2026, 8, 2, 12, 0));
        data.setEndTime(LocalDateTime.of(2026, 8, 2, 14, 0));
        return data;
    }
}
