package com.rally.contentproduction.dailyschedulecopy.activity;

import com.rally.domain.translation.model.TranslationKey;

import java.util.LinkedHashSet;
import java.util.Set;

/** A5 活动返回的纯文本与去重缺译键集合。 */
public record ComposeDailyScheduleCopyResult(String copy, Set<TranslationKey> missingTranslationKeys) {

    public ComposeDailyScheduleCopyResult {
        copy = copy == null ? "" : copy;
        missingTranslationKeys = missingTranslationKeys == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(missingTranslationKeys));
    }
}
