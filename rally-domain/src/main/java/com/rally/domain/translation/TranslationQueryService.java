package com.rally.domain.translation;

import com.rally.domain.translation.cache.TranslationCache;
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

@Slf4j
@Service
public class TranslationQueryService {

    @Resource
    private TranslationGateway translationGateway;

    @Resource
    private TranslationCache translationCache;

    /**
     * 单条翻译查询
     * - 命中缓存 → 直接返回
     * - 数据库有记录且 translatedText 非空 → 返回译文并写缓存
     * - 数据库有记录但 translatedText 为空 → 返回原文并写缓存
     * - 数据库无记录 → 新建占位记录，返回原文并写缓存
     */
    public String translate(TranslationLanguageEnum language, String text, TranslationEntityTypeEnum entityType) {
        if (text == null || text.isBlank()) return text;

        String cached = translationCache.get(entityType, text, language);
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
            translationCache.put(entityType, text, language, text);
            return text;
        }

        String result = data.getTranslatedText() != null ? data.getTranslatedText() : text;
        translationCache.put(entityType, text, language, result);
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
            String cached = translationCache.get(q.getEntityType(), q.getOriginalText(), q.getLanguage());
            if (cached != null) {
                result.put(buildResultKey(q), cached);
            } else {
                missedQueries.add(q);
            }
        }

        if (missedQueries.isEmpty()) return result;

        List<TranslationData> dbResults = translationGateway.findBatch(missedQueries);
        Map<String, TranslationData> dbMap = new HashMap<>();
        for (TranslationData d : dbResults) {
            dbMap.put(buildResultKey(d), d);
        }

        List<TranslationData> toSave = new ArrayList<>();
        for (TranslationData q : missedQueries) {
            String key = buildResultKey(q);
            TranslationData dbData = dbMap.get(key);

            if (dbData == null) {
                TranslationData newData = new TranslationData();
                newData.setEntityType(q.getEntityType());
                newData.setOriginalText(q.getOriginalText());
                newData.setLanguage(q.getLanguage());
                toSave.add(newData);
                translationCache.put(q.getEntityType(), q.getOriginalText(), q.getLanguage(), q.getOriginalText());
                result.put(key, q.getOriginalText());
            } else {
                String translated = dbData.getTranslatedText() != null ? dbData.getTranslatedText() : q.getOriginalText();
                translationCache.put(q.getEntityType(), q.getOriginalText(), q.getLanguage(), translated);
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

    /** translateBatch 返回 Map 的 key，与缓存 key 解耦，仅用于方法内部聚合结果 */
    private String buildResultKey(TranslationData data) {
        return data.getEntityType().name() + ":" + data.getOriginalText() + ":" + data.getLanguage().name();
    }
}
