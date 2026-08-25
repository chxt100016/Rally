package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import lombok.Data;

import java.util.List;

/**
 * 改写球场资料命令（C3）：字段为 null 表示不改。
 * alias 与 tags 传空列表表示清空，传 null 表示不改。
 */
@Data
public class CourtUpdateCmd {
    private String courtId;
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
