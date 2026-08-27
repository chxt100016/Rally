package com.rally.platformconfig.homecontentquery.activity;

import com.rally.domain.translation.model.TranslationKey;
import com.rally.home.model.HomeDisplayItemDTO;

import java.util.LinkedHashSet;
import java.util.Set;

/** 首页巡回赛区块及本次展示中仍缺失的简中翻译键。 */
public record QueryHomeTourSectionResult(
        HomeDisplayItemDTO displayItem,
        Set<TranslationKey> missingTranslationKeys) {

    public QueryHomeTourSectionResult {
        missingTranslationKeys = missingTranslationKeys == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(missingTranslationKeys));
    }

    public static QueryHomeTourSectionResult empty() {
        return new QueryHomeTourSectionResult(null, Set.of());
    }
}
