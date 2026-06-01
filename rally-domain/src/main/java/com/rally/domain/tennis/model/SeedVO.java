package com.rally.domain.tennis.model;

import lombok.Data;

@Data
public class SeedVO {

    private CountryVO country;
    private String playerId;
    private String name;
    private Integer seed;
    private SeedStatusEnum status;
    /** 被淘汰的轮次中文名，如"8强"；仅 ELIMINATED 时有值 */
    private String label;
    /** 所属赛事 ID */
    private String tournamentId;
    /** 赛事类型，如 ATP / WTA */
    private String tour;
}
