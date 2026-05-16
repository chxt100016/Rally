package com.rally.translation;

import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * app 层门面，委托到 domain 层 TranslationQueryService，
 * 保持对 adapter/job 层的调用接口不变
 */
@Service
public class TennisTranslationService {

    @Resource
    private TranslationQueryService delegate;

    public String translate(TranslationLanguageEnum language, String text, TranslationEntityTypeEnum entityType) {
        return delegate.translate(language, text, entityType);
    }

    public Map<String, String> translateBatch(List<TranslationData> queries) {
        return delegate.translateBatch(queries);
    }
}
