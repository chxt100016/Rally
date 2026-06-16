package com.rally.domain.user.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 删除视频入参
 */
@Data
public class DeleteVideoCmd {
    /** 视频 key（存储标识） */
    @NotBlank(message = "视频 key 不能为空")
    private String key;
}
