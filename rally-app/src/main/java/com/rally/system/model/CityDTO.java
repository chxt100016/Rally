package com.rally.system.model;

import lombok.Data;

/**
 * 城市 DTO
 */
@Data
public class CityDTO {
    private String code;
    private String name;
    private String initials;
    private String pinyin;
}
