package com.rally.domain.translation.gateway;

import com.rally.domain.translation.model.TranslationData;

import java.util.List;

public interface TranslationRepository {

    /** 批量查询已存在的翻译记录（不论 translatedText 是否为 null） */
    List<TranslationData> findBatch(List<TranslationData> queries);

    /** 新建翻译记录（translatedText 为 null，等待批量翻译填充） */
    void save(TranslationData data);

    /** 批量新建翻译记录 */
    void saveBatch(List<TranslationData> dataList);

    /** 批量更新翻译结果 */
    void updateBatchTranslatedText(List<TranslationData> dataList);

    /** 查询所有 translatedText 为 null 的待翻译记录 */
    List<TranslationData> findAllPending();
}
