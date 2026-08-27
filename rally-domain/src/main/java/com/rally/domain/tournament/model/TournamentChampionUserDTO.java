package com.rally.domain.tournament.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

/**
 * 赛事冠军成员展示信息。
 */
@Data
public class TournamentChampionUserDTO {
    private String userId;
    private String name;
    private String avatarUrl;
    private GenderEnum gender;
}
