package com.rally.domain.tennis.model;

import lombok.Data;

@Data
public class SeedVO {

    private CountryVO country;
    private String playerId;
    private String name;
    private Integer seed;
    private SeedStatusEnum status;
    /** 所属赛事 ID */
    private String tournamentId;
    /** 赛事类型，如 ATP / WTA */
    private String tour;
}
