package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import lombok.Data;

import java.util.List;

/**
 * 录入球场命令（C1）：运营手工录入一条球场。
 * 地理归属由调用方按名录解析后传入。
 */
@Data
public class CourtCreateCmd {
    private String name;
    private List<String> alias;
    private String address;
    private Double lng;
    private Double lat;
    private CourtLocation location;
    private String remark;
    private CourtEnvironmentEnum type;
    private CourtSurfaceEnum surface;
    private List<String> tags;
    private CourtProfile profile;
    private CourtStatusEnum status;
}
