package com.rally.db.translation.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.db.translation.entity.TranslationPO;
import com.rally.db.translation.service.TranslationMybatisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TranslationRepository {

    private final TranslationMybatisService translationMybatisService;

    public TranslationPO findOne(String entityType, String originalText, String language) {
        return translationMybatisService.lambdaQuery()
                .eq(TranslationPO::getEntityType, entityType)
                .eq(TranslationPO::getOriginalText, originalText)
                .eq(TranslationPO::getLanguage, language)
                .one();
    }

    /**
     * 批量查询：传入多个 (entityType, originalText, language) 组合，用 OR 拼接
     * 适合小批量（< 200 条）
     */
    public List<TranslationPO> findBatch(List<TranslationPO> queries) {
        if (queries.isEmpty()) return List.of();
        LambdaQueryWrapper<TranslationPO> wrapper = new LambdaQueryWrapper<>();
        for (int i = 0; i < queries.size(); i++) {
            TranslationPO q = queries.get(i);
            if (i > 0) wrapper.or();
            wrapper.nested(w -> w
                    .eq(TranslationPO::getEntityType, q.getEntityType())
                    .eq(TranslationPO::getOriginalText, q.getOriginalText())
                    .eq(TranslationPO::getLanguage, q.getLanguage()));
        }
        return translationMybatisService.list(wrapper);
    }

    public void save(TranslationPO po) {
        translationMybatisService.save(po);
    }

    public void saveBatch(List<TranslationPO> list) {
        translationMybatisService.saveBatch(list);
    }

    public void updateBatchById(List<TranslationPO> list) {
        translationMybatisService.updateBatchById(list);
    }

    public List<TranslationPO> findAllPending() {
        return translationMybatisService.lambdaQuery()
                .isNull(TranslationPO::getTranslatedText)
                .list();
    }
}
