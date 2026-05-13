package com.rally.db.translation.gateway;

import com.rally.db.translation.entity.TranslationPO;
import com.rally.db.translation.repository.TranslationRepository;
import com.rally.domain.translation.gateway.TranslationGateway;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TranslationGatewayImpl implements TranslationGateway {

    private final TranslationRepository translationRepository;

    @Override
    public TranslationData findOne(TranslationEntityTypeEnum entityType, String originalText, TranslationLanguageEnum language) {
        TranslationPO po = translationRepository.findOne(entityType.name(), originalText, language.name());
        return po == null ? null : toDomain(po);
    }

    @Override
    public List<TranslationData> findBatch(List<TranslationData> queries) {
        List<TranslationPO> poQueries = queries.stream().map(this::toQueryPO).toList();
        return translationRepository.findBatch(poQueries).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(TranslationData data) {
        translationRepository.save(toPO(data));
    }

    @Override
    public void saveBatch(List<TranslationData> dataList) {
        translationRepository.saveBatch(dataList.stream().map(this::toPO).toList());
    }

    @Override
    public void updateBatchTranslatedText(List<TranslationData> dataList) {
        List<TranslationPO> list = dataList.stream().map(d -> {
            TranslationPO po = new TranslationPO();
            po.setId(d.getId());
            po.setTranslatedText(d.getTranslatedText());
            return po;
        }).toList();
        translationRepository.updateBatchById(list);
    }

    @Override
    public List<TranslationData> findAllPending() {
        return translationRepository.findAllPending().stream().map(this::toDomain).toList();
    }

    private TranslationData toDomain(TranslationPO po) {
        TranslationData d = new TranslationData();
        d.setId(po.getId());
        d.setEntityType(TranslationEntityTypeEnum.valueOf(po.getEntityType()));
        d.setOriginalText(po.getOriginalText());
        d.setLanguage(TranslationLanguageEnum.valueOf(po.getLanguage()));
        d.setTranslatedText(po.getTranslatedText());
        return d;
    }

    private TranslationPO toPO(TranslationData d) {
        TranslationPO po = new TranslationPO();
        po.setId(d.getId());
        if (d.getEntityType() != null) po.setEntityType(d.getEntityType().name());
        po.setOriginalText(d.getOriginalText());
        if (d.getLanguage() != null) po.setLanguage(d.getLanguage().name());
        po.setTranslatedText(d.getTranslatedText());
        return po;
    }

    /** 仅用于批量查询条件构造，不含 id */
    private TranslationPO toQueryPO(TranslationData d) {
        TranslationPO po = new TranslationPO();
        if (d.getEntityType() != null) po.setEntityType(d.getEntityType().name());
        po.setOriginalText(d.getOriginalText());
        if (d.getLanguage() != null) po.setLanguage(d.getLanguage().name());
        return po;
    }
}
