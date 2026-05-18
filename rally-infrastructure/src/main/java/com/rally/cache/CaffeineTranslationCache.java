package com.rally.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.rally.db.translation.convert.TranslationConvertMapper;
import com.rally.db.translation.entity.TranslationPO;
import com.rally.db.translation.repository.TranslationRepository;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationKey;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CaffeineTranslationCache implements TranslationCache {

    private final TranslationRepository translationRepository;
    private final LoadingCache<TranslationKey, String> cache;

    public CaffeineTranslationCache(TranslationRepository translationRepository) {
        this.translationRepository = translationRepository;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                // 缓存未命中时按 key 从 DB 加载单条，返回 null 表示无翻译
                .build(this::loadByKey);
        loadAll();
    }

    @Override
    public String get(TranslationKey key) {
        // LoadingCache.get() 在 key 缺失时自动调用 loadByKey
        return cache.get(key);
    }

    /** 全量失效后立即重新加载 */
    @Override
    public void invalidate() {
        cache.invalidateAll();
        loadAll();
    }

    private String loadByKey(TranslationKey key) {
        TranslationPO queryPO = TranslationConvertMapper.INSTANCE.toQueryPO(key);
        TranslationPO po = translationRepository.findOne(
                queryPO.getEntityType(), queryPO.getOriginalText(), queryPO.getLanguage());
        return po != null ? po.getTranslatedText() : null;
    }

    private void loadAll() {
        Map<TranslationKey, String> entries = TranslationConvertMapper.INSTANCE
                .toDomainList(translationRepository.findAllTranslated())
                .stream()
                .collect(Collectors.toMap(
                        d -> new TranslationKey(d.getEntityType(), d.getOriginalText(), d.getLanguage()),
                        TranslationData::getTranslatedText));
        cache.putAll(entries);
    }
}
