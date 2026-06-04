package com.rally.domain.meetup.model;

import lombok.Data;

/**
 * GEO 查询返回结果
 */
@Data
public class NearbyResult {
    /** 约球 ID */
    private String meetupId;
    /** 距离（米） */
    private Double distanceMeters;
}
