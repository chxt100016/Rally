package com.rally.domain.recap.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkipReviewCmd {
    @NotBlank(message = "meetupId 不能为空")
    private String meetupId;
}
