package com.rally.domain.tournament.model;

import lombok.Data;

/**
 * 赛事参赛者拒绝比赛次数统计
 */
@Data
public class TournamentRejectRecordDTO {
    private String userId;
    private String nickname;
    private Integer rejectCount;
}
