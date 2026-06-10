package com.rally.domain.court.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 球场查询入参
 */
@Data
public class CourtQueryCmd {
    @NotBlank(message = "请选择城市")
    private String cityCode;
    private String keyword;
    private Integer pageNo = 1;
    private Integer pageSize = 20;
}
