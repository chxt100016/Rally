package com.rally.domain.system.model;

import lombok.Data;

/**
 * 城市领域数据模型
 */
@Data
public class Location {
    private String code;
    private String name;
    private String initials;
    private String pinyin;
}
