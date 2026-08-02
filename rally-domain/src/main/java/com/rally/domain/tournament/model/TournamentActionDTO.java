package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import lombok.Data;

/**
 * 当前用户的赛事待办状态及展示文案。
 */
@Data
public class TournamentActionDTO {
    private TournamentActionStateEnum state;
    private String stateShow;
    private String stateTitle;
    private String stateSubtitle;
}
