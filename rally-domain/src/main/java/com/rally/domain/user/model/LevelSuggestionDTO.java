package com.rally.domain.user.model;

import lombok.Data;

/**
 * 我的档案 - NTRP 系统建议
 */
@Data
public class LevelSuggestionDTO {

    /** 建议调整到 */
    private String to;

    /** 建议内容 */
    private String content;
}
