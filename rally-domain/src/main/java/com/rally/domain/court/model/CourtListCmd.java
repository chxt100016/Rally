package com.rally.domain.court.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourtListCmd {
    @NotBlank(message = "请选择城市")
    private String cityCode;
}
