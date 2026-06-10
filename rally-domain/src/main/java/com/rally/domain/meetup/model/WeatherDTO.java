package com.rally.domain.meetup.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 天气信息 DTO
 */
@Data
@Accessors(chain = true)
public class WeatherDTO {
    /** 日出时间（HH:mm） */
    private String sunrise;
    /** 日落时间（HH:mm） */
    private String sunset;
}
