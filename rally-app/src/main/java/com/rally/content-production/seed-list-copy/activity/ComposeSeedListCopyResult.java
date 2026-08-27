package com.rally.contentproduction.seedlistcopy.activity;

import com.rally.domain.translation.model.TranslationKey;

import java.util.LinkedHashSet;
import java.util.Set;

/** compose-seed-list-copy 活动返回的纯文本与去重缺译键集合。 */
public record ComposeSeedListCopyResult(String copy, Set<TranslationKey> missingTranslations) {

    public ComposeSeedListCopyResult {
        copy = copy == null ? "" : copy;
        missingTranslations = missingTranslations == null
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(missingTranslations));
    }
}
