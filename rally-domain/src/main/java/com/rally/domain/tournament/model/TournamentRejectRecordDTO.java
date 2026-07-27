package com.rally.domain.tournament.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

/**
 * 赛事参赛者拒绝比赛次数统计
 */
@Data
public class TournamentRejectRecordDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
    private Integer rejectCount;
}
