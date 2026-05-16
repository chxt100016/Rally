package com.rally.db.translation.gateway;

import com.rally.db.translation.convert.TranslationConvertMapper;
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

    private static final TranslationConvertMapper MAPPER = TranslationConvertMapper.INSTANCE;

    @Override
    public TranslationData findOne(TranslationEntityTypeEnum entityType, String originalText, TranslationLanguageEnum language) {
        TranslationPO po = translationRepository.findOne(entityType.name(), originalText, language.name());
        return MAPPER.toDomain(po);
    }

    @Override
    public List<TranslationData> findBatch(List<TranslationData> queries) {
        List<TranslationPO> poQueries = queries.stream().map(MAPPER::toQueryPO).toList();
        return MAPPER.toDomainList(translationRepository.findBatch(poQueries));
    }

    @Override
    public void save(TranslationData data) {
        translationRepository.save(MAPPER.toPO(data));
    }

    @Override
    public void saveBatch(List<TranslationData> dataList) {
        translationRepository.saveBatch(dataList.stream().map(MAPPER::toPO).toList());
    }

    @Override
    public void updateBatchTranslatedText(List<TranslationData> dataList) {
        // 仅更新 id + translatedText，避免覆盖其他字段
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
        return MAPPER.toDomainList(translationRepository.findAllPending());
    }
}
