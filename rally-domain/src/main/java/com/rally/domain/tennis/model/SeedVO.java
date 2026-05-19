package com.rally.domain.tennis.model;

import lombok.Data;

@Data
public class SeedVO {

    private CountryVO country;
    private String playerId;
    private String name;
    private Integer seed;
    private SeedStatusEnum status;
}
