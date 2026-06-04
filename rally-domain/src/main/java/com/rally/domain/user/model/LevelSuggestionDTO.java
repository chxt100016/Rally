package com.rally.domain.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 我的档案 - NTRP 系统建议
 */
@Data
@Accessors(chain = true)
public class LevelSuggestionDTO {

    /** 建议调整到 */
    private String to;

    /** 建议内容 */
    private String content;
}
