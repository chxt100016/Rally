package com.rally.domain.translation;

import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.gateway.TranslationGateway;
import com.rally.domain.translation.model.TranslationData;
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

        // 用 Map 去重，避免 queries 中有重复条目时插入重复记录
        Map<String, TranslationData> toSaveMap = new HashMap<>();
        for (TranslationData q : missedQueries) {
            String key = buildResultKey(q);
            TranslationData dbData = dbMap.get(key);

            if (dbData == null) {
                TranslationData newData = new TranslationData();
                newData.setEntityType(q.getEntityType());
                newData.setOriginalText(q.getOriginalText());
                newData.setLanguage(q.getLanguage());
                toSaveMap.put(key, newData);
                translationCache.put(q.getEntityType(), q.getOriginalText(), q.getLanguage(), q.getOriginalText());
                result.put(key, q.getOriginalText());
            } else {
                String translated = dbData.getTranslatedText() != null ? dbData.getTranslatedText() : q.getOriginalText();
                translationCache.put(q.getEntityType(), q.getOriginalText(), q.getLanguage(), translated);
                result.put(key, translated);
            }
        }

        if (!toSaveMap.isEmpty()) {
            try {
                translationGateway.saveBatch(new ArrayList<>(toSaveMap.values()));
            } catch (Exception e) {
                log.warn("批量保存翻译记录部分冲突，忽略: count={}", toSaveMap.size(), e);
            }
        }

        return result;
    }

    /** translateBatch 返回 Map 的 key，与缓存 key 解耦，仅用于方法内部聚合结果 */
    private String buildResultKey(TranslationData data) {
        return data.getEntityType().name() + ":" + data.getOriginalText() + ":" + data.getLanguage().name();
    }
}
