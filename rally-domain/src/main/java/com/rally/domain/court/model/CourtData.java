package com.rally.domain.court.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 球场领域数据对象
 */
@Data
public class CourtData {
    private String bizId;
    private String name;
    private String address;
    private Double lng;
    private Double lat;
    private String cityCode;
    private String districtCode;
    private Integer total;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
