package com.rally.domain.user.model;

import com.rally.domain.meetup.model.MeetupCardDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 我的档案 - 约球信息
 */
@Data
@Accessors(chain = true)
public class PlayerHomeMeetupDTO {

    /** 已完成约球次数 */
    private Integer completedCount;

    /** 最近 3 场比赛 */
    private List<MeetupCardDTO> recentMeetups;
}
