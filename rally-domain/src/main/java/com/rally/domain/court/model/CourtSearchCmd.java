package com.rally.domain.court.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourtSearchCmd {
    @NotBlank(message = "请选择城市")
    private String cityCode;
    private String query;
}
