package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 球员信息 VO
 */
@Data
public class PlayerVO {

    private String id;
    private String name;
    private CountryVO country;
    private Integer seed;
}
