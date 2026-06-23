package com.rally.domain.tour.model;

import lombok.Data;

/**
 * 签表数据模型（domain 层）
 */
@Data
public class TourDrawData {

    private Long id;
    private String tournamentId;
    private Integer year;
    private String drawType;
    /** 签表人数：32 / 64 / 128 */
    private Integer size;
    private Integer totalRounds;
}
