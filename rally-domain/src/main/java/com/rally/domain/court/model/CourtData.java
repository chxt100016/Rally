package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtSourceEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 球场领域数据对象
 */
@Data
public class CourtData {
    private String bizId;
    private String name;
    private String alias;
    private String address;
    private Double lng;
    private Double lat;
    private String cityCode;
    private String districtCode;
    private Integer total;
    private String remark;
    private CourtEnvironmentEnum type;
    private String tags;
    private String cityName;
    private String districtName;
    private String extData;
    private CourtSourceEnum source;
    private CourtStatusEnum status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
