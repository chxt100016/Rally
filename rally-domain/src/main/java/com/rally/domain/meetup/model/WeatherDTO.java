package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 天气信息 DTO
 */
@Data
@Accessors(chain = true)
public class WeatherDTO {
    /** 日出时间（HH:mm） */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sunrise;
    /** 日落时间（HH:mm） */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sunset;
}
