package com.rally.domain.court.model;

import lombok.Data;

/**
 * 球场对外返回 DTO
 */
@Data
public class CourtDTO {
    private String courtId;
    private String name;
    private String address;
    private Double lng;
    private Double lat;
    private String cityCode;
    private String districtCode;
    private Integer total;
    private String remark;
}
