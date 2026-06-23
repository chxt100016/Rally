package com.rally.domain.tour.model;

import lombok.Data;

import java.time.LocalDate;

/**
 * 球员详细数据模型（domain 层），含排名、积分、出生日期
 */
@Data
public class PlayerDetailData {

    private String playerId;
    private String firstName;
    private String lastName;
    private String nationality;
    private Integer rank;
    private Integer points;
    private LocalDate birthDate;
}
