package com.rally.domain.translation.model;

import lombok.Getter;

@Getter
public enum TranslationEntityTypeEnum {

    COURT("球场", null),
    PLAYER("球员", "球员姓名通常音译，优先使用 ATP/WTA 官方中文译名"),
    TOURNAMENT("赛事", null),
    SURFACE("场地类型", null),
    CITY("城市", null);

    private final String chineseDesc;
    /** 给 AI 的额外提示，null 表示无需额外说明 */
    private final String hint;

    TranslationEntityTypeEnum(String chineseDesc, String hint) {
        this.chineseDesc = chineseDesc;
        this.hint = hint;
    }
}
