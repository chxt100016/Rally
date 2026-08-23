package com.rally.domain.system.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 运营后台可编辑的单项首页配置。 */
@Data
@AllArgsConstructor
public class HomeConfigItemDTO {
    private String key;
    private String description;
    private String configValue;
    private String defaultValue;
    private Integer version;
    private Boolean overridden;
}
