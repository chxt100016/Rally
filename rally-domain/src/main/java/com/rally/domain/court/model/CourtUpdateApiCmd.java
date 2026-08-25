package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 球场编辑入参。字段为 null 表示不改；alias 与 tags 传空列表表示清空。
 */
@Data
public class CourtUpdateApiCmd {

    @NotBlank(message = "请选择球场")
    private String courtId;

    @Size(max = 128, message = "球场名称过长")
    private String name;

    private String cityCode;

    private List<String> alias;

    @Size(max = 256, message = "球场地址过长")
    private String address;

    @DecimalMin(value = "-180", message = "经度不正确")
    @DecimalMax(value = "180", message = "经度不正确")
    private Double lng;

    @DecimalMin(value = "-90", message = "纬度不正确")
    @DecimalMax(value = "90", message = "纬度不正确")
    private Double lat;

    private String districtCode;

    @Size(max = 255, message = "备注过长")
    private String remark;

    private CourtEnvironmentEnum type;

    private CourtSurfaceEnum surface;

    private List<String> tags;

    private String rating;

    private String cost;

    private String opentime;

    private String tel;

    private CourtStatusEnum status;
}
