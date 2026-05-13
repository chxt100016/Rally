package com.rally.domain.translation.model;

import lombok.Getter;

@Getter
public enum TranslationLanguageEnum {

    ZH_CN("简体中文"),
    ZH_TW("繁体中文"),
    EN("英语"),
    JA("日语"),
    KO("韩语");

    private final String chineseDesc;

    TranslationLanguageEnum(String chineseDesc) {
        this.chineseDesc = chineseDesc;
    }
}
