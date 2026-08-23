package com.rally.domain.system.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新一项首页配置。 */
@Data
public class HomeConfigUpdateCmd {
    @NotBlank(message = "配置 key 不能为空")
    private String key;
    @NotBlank(message = "配置内容不能为空")
    private String configValue;
    @NotNull(message = "配置版本不能为空")
    private Integer version;
}
