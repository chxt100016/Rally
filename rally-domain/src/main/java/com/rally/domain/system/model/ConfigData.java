package com.rally.domain.system.model;

import com.rally.domain.system.enums.ValueType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置领域数据模型
 */
@Data
public class ConfigData {
    private String bizId;
    private String configKey;
    private String configValue;
    private ValueType valueType;
    private String scope;
    private String description;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
