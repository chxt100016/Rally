package com.rally.domain.translation.cache;

import com.rally.domain.translation.model.TranslationKey;

public interface TranslationCache {

    String get(TranslationKey key);

    /** 全量失效，触发后台重新加载 */
    void invalidate();
}
