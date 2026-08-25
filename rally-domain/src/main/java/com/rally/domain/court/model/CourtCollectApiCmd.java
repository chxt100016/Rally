package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtCollectModeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 球场数据抓取收录入参
 */
@Data
public class CourtCollectApiCmd {

    @NotBlank(message = "请选择城市")
    private String cityCode;

    @NotNull(message = "抓取模式不正确")
    private CourtCollectModeEnum mode;
}
