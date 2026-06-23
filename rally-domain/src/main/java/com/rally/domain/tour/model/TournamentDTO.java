package com.rally.domain.tour.model;

import lombok.Data;

/**
 * 赛事查询响应 VO
 */
@Data
public class TournamentDTO {

    private String id;
    private String name;
    private String type;
    private String typeLabel;
    private String category;
    private String surface;
    private String surfaceLabel;
    private String city;
    private String startDate;
    private String endDate;
    private String status;
    private String statusLabel;
    private String groupId;
    private String backgroundUrl;
}
