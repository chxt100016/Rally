package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtSourceEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
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
    /** 场地材质：HARD/CLAY/GRASS */
    private CourtSurfaceEnum surface;
    private String tags;
    private String cityName;
    private String districtName;
    private String extData;
    private CourtSourceEnum source;
    /** 三方来源编号，高德兴趣点编号 */
    private String sourceId;
    private CourtStatusEnum status;
    private Integer meetupCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
