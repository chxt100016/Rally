package com.rally.domain.translation.model;

import lombok.Getter;

@Getter
public enum TranslationEntityTypeEnum {

    COURT("球场", null),
    PLAYER("球员", "球员姓名通常音译，优先使用 ATP/WTA 官方中文译名"),
    TOURNAMENT("赛事", "不用完整翻译全部文案，赛事通常有标准名称， 优先使用 ATP/WTA 官方中文名称， 如果你不知道官方中文名称， 而是使用翻译的情况下只要保留简洁的赛事名称。 因为在展示的时候可能会出现同一个赛事多个赛事类型合并的场景（ATP、WTA), 所以不要翻译出赛事类型而是更加通用的类型， 比如：罗马网球公开赛、温布尔登网球公开赛"),
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
