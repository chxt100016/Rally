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
 * 球场新增入参
 */
@Data
public class CourtCreateApiCmd {

    @NotBlank(message = "请填写球场名称")
    @Size(max = 128, message = "球场名称过长")
    private String name;

    @NotBlank(message = "请选择城市")
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
