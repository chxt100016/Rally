package com.rally.domain.court.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 球场停用入参
 */
@Data
public class CourtDisableApiCmd {

    @NotBlank(message = "请选择球场")
    private String courtId;
}
