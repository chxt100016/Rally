package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import lombok.Data;

import java.util.List;

/**
 * 收录球场命令（C2）与按抓取结果覆盖球场命令（C5）共用的入参。
 * 地理归属由调用方按名录解析后传入；备注、场地材质、约球次数不在覆盖范围内。
 */
@Data
public class CourtCollectCmd {
    private String sourceId;
    private String name;
    private List<String> alias;
    private String address;
    private Double lng;
    private Double lat;
    private CourtLocation location;
    private CourtEnvironmentEnum type;
    private List<String> tags;
    private CourtProfile profile;
}
