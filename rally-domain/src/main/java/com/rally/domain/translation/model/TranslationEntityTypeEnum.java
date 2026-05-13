package com.rally.domain.translation.model;

import lombok.Getter;

@Getter
public enum TranslationEntityTypeEnum {

    COURT("球场"),
    PLAYER("球员"),
    TOURNAMENT("赛事"),
    SURFACE("场地类型");

    private final String chineseDesc;

    TranslationEntityTypeEnum(String chineseDesc) {
        this.chineseDesc = chineseDesc;
    }
}
