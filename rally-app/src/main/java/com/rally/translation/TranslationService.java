package com.rally.translation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rally.domain.translation.gateway.TranslationGateway;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TranslationService {

    @Resource
    private TranslationGateway translationGateway;

    /**
     * 本地缓存：key = "entityType:originalText:language"，value = 翻译结果（原文或译文）
     * TTL 30 分钟，最大 10000 条
     */
    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * 单条翻译查询
     * - 命中缓存 → 直接返回
     * - 数据库有记录且 translatedText 非空 → 返回译文并写缓存
     * - 数据库有记录但 translatedText 为空 → 返回原文并写缓存
     * - 数据库无记录 → 新建占位记录，返回原文并写缓存
     */
    public String translate(TranslationLanguageEnum language, String text, TranslationEntityTypeEnum entityType) {
        if (text == null || text.isBlank()) return text;

        String cacheKey = buildCacheKey(entityType, text, language);
        String cached = localCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        TranslationData data = translationGateway.findOne(entityType, text, language);

        if (data == null) {
            TranslationData newData = new TranslationData();
            newData.setEntityType(entityType);
            newData.setOriginalText(text);
            newData.setLanguage(language);
            try {
                translationGateway.save(newData);
            } catch (Exception e) {
                // 并发场景下可能唯一键冲突，忽略
                log.warn("保存翻译记录冲突，忽略: entityType={}, text={}, lang={}", entityType, text, language);
            }
            localCache.put(cacheKey, text);
            return text;
        }

        String result = data.getTranslatedText() != null ? data.getTranslatedText() : text;
        localCache.put(cacheKey, result);
        return result;
    }

    /**
     * 批量翻译查询，先查缓存，再一次性查数据库，减少 DB 往返
     * 返回 Map，key = "entityType:originalText:language"，value = 翻译结果
     */
    public Map<String, String> translateBatch(List<TranslationData> queries) {
        Map<String, String> result = new HashMap<>();
        List<TranslationData> missedQueries = new ArrayList<>();

        for (TranslationData q : queries) {
            String key = buildCacheKey(q.getEntityType(), q.getOriginalText(), q.getLanguage());
            String cached = localCache.getIfPresent(key);
            if (cached != null) {
                result.put(key, cached);
            } else {
                missedQueries.add(q);
            }
        }

        if (missedQueries.isEmpty()) return result;

        List<TranslationData> dbResults = translationGateway.findBatch(missedQueries);
        Map<String, TranslationData> dbMap = new HashMap<>();
        for (TranslationData d : dbResults) {
            dbMap.put(buildCacheKey(d.getEntityType(), d.getOriginalText(), d.getLanguage()), d);
        }

        List<TranslationData> toSave = new ArrayList<>();
        for (TranslationData q : missedQueries) {
            String key = buildCacheKey(q.getEntityType(), q.getOriginalText(), q.getLanguage());
            TranslationData dbData = dbMap.get(key);

            if (dbData == null) {
                TranslationData newData = new TranslationData();
                newData.setEntityType(q.getEntityType());
                newData.setOriginalText(q.getOriginalText());
                newData.setLanguage(q.getLanguage());
                toSave.add(newData);
                localCache.put(key, q.getOriginalText());
                result.put(key, q.getOriginalText());
            } else {
                String translated = dbData.getTranslatedText() != null ? dbData.getTranslatedText() : q.getOriginalText();
                localCache.put(key, translated);
                result.put(key, translated);
            }
        }

        if (!toSave.isEmpty()) {
            try {
                translationGateway.saveBatch(toSave);
            } catch (Exception e) {
                log.warn("批量保存翻译记录部分冲突，忽略: count={}", toSave.size());
            }
        }

        return result;
    }

    /** 批量翻译完成后清除相关缓存，让下次查询读到最新译文 */
    public void invalidateCache(List<TranslationData> updated) {
        for (TranslationData d : updated) {
            localCache.invalidate(buildCacheKey(d.getEntityType(), d.getOriginalText(), d.getLanguage()));
        }
    }

    private String buildCacheKey(TranslationEntityTypeEnum entityType, String text, TranslationLanguageEnum language) {
        return entityType.name() + ":" + text + ":" + language.name();
    }
}
