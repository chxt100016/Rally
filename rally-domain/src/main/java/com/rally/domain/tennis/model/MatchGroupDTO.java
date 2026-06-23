package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

@Data
public class MatchGroupDTO {
    /** 分组原始值：日期为 yyyy-MM-dd，球场为球场名，轮次为枚举 roundName */
    private String key;
    /** 分组展示名：日期为 今天/明天/日期，球场为球场名，轮次为中文 label */
    private String name;
    private List<MatchQueryVO> data;
    private List<MatchGroupDTO> children;
}
