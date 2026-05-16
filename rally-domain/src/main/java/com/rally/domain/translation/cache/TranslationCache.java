package com.rally.domain.translation.cache;

import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;

import java.util.List;

public interface TranslationCache {

    /** 查询缓存，未命中返回 null */
    String get(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language);

    void put(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language, String value);

    /** 批量失效，翻译结果写库后调用 */
    void invalidateBatch(List<TranslationData> dataList);
}
