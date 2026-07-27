package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

/**
 * 赛事报名人员信息
 */
@Data
public class TournamentEntrantDTO {
    private String userId;
    private Integer entryNo;
    private String entryNoShow;
    private TournamentEntryStatusEnum status;
    private String statusShow;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
}
