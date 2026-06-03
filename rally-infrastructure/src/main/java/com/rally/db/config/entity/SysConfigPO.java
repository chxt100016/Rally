package com.rally.db.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfigPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String configKey;
    private String configValue;
    private String valueType;
    private String scope;
    private String description;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
