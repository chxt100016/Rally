package com.rally.db.translation.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.translation.convert.TranslationConvertMapper;
import com.rally.db.translation.entity.TranslationPO;
import com.rally.db.translation.service.TranslationMybatisService;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TranslationRepositoryImpl implements TranslationRepository {

    private final TranslationMybatisService translationMybatisService;
    private static final TranslationConvertMapper MAPPER = TranslationConvertMapper.INSTANCE;

    @Override
    public List<TranslationData> findBatch(List<TranslationData> queries) {
        return MAPPER.toDomainList(findBatchPO(queries.stream().map(MAPPER::toQueryPO).toList()));
    }

    @Override
    public void save(TranslationData data) {
        translationMybatisService.save(MAPPER.toPO(data));
    }

    @Override
    public void saveBatch(List<TranslationData> dataList) {
        translationMybatisService.saveBatch(dataList.stream().map(MAPPER::toPO).toList());
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
        translationMybatisService.updateBatchById(list);
    }

    @Override
    public List<TranslationData> findAllPending() {
        List<TranslationPO> poList = translationMybatisService.lambdaQuery()
                .eq(TranslationPO::getTranslatedText, "")
                .list();
        return MAPPER.toDomainList(poList);
    }

    /**
     * 批量查询：传入多个 (entityType, originalText, language) 组合，用 OR 拼接，适合小批量（< 200 条）
     */
    private List<TranslationPO> findBatchPO(List<TranslationPO> queries) {
        if (queries.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<TranslationPO> wrapper = new LambdaQueryWrapper<>();
        for (int i = 0; i < queries.size(); i++) {
            TranslationPO q = queries.get(i);
            if (i > 0) {
                wrapper.or();
            }
            wrapper.nested(w -> w
                    .eq(TranslationPO::getEntityType, q.getEntityType())
                    .eq(TranslationPO::getOriginalText, q.getOriginalText())
                    .eq(TranslationPO::getLanguage, q.getLanguage()));
        }
        return translationMybatisService.list(wrapper);
    }
}
