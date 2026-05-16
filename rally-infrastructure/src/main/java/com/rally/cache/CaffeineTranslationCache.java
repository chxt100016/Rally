package com.rally.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** 基于 Caffeine 的翻译本地缓存实现，TTL 30 分钟，最大 10000 条 */
@Component
public class CaffeineTranslationCache implements TranslationCache {

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    @Override
    public String get(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language) {
        return cache.getIfPresent(buildKey(entityType, text, language));
    }

    @Override
    public void put(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language, String value) {
        cache.put(buildKey(entityType, text, language), value);
    }

    @Override
    public void invalidateBatch(List<TranslationData> dataList) {
        for (TranslationData d : dataList) {
            cache.invalidate(buildKey(d.getEntityType(), d.getOriginalText(), d.getLanguage()));
        }
    }

    private String buildKey(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language) {
        return entityType.name() + ":" + text + ":" + language.name();
    }
}
