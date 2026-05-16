package com.rally.domain.translation;

import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.gateway.TranslationClient;
import com.rally.domain.translation.gateway.TranslationGateway;
import com.rally.domain.translation.model.TranslationData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TranslationService {

    @Resource
    private TranslationGateway translationGateway;

    @Resource
    private TranslationClient translationClient;

    @Resource
    private TranslationCache translationCache;

    /** 每批次最多发送给翻译客户端的条数，避免单次请求过大 */
    private static final int BATCH_SIZE = 50;

    /**
     * 执行批量翻译：
     * 1. 查询所有 translatedText 为 null 的待翻译记录
     * 2. 分批调用翻译 Client
     * 3. 将翻译结果写回数据库并清除本地缓存
     *
     * @return 本次翻译成功的条数
     */
    public int batch() {
        List<TranslationData> pending = translationGateway.findAllPending();
        if (pending.isEmpty()) {
            log.info("无待翻译记录");
            return 0;
        }
        log.info("待翻译记录数: {}", pending.size());

        int successCount = 0;
        for (int i = 0; i < pending.size(); i += BATCH_SIZE) {
            List<TranslationData> batch = pending.subList(i, Math.min(i + BATCH_SIZE, pending.size()));
            successCount += processBatch(batch);
        }

        log.info("批量翻译完成，成功: {}/{}", successCount, pending.size());
        return successCount;
    }

    /** 直接翻译并返回译文列表（供 Controller 透传调用） */
    public List<String> process(List<TranslationData> batch) {
        return translationClient.translate(batch);
    }

    public int processBatch(List<TranslationData> batch) {
        List<String> results = translationClient.translate(batch);
        if (results == null) {
            log.error("翻译失败，跳过本批次 {} 条", batch.size());
            return 0;
        }

        List<TranslationData> toUpdate = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            String translated = results.get(i).trim();
            if (!translated.isBlank()) {
                TranslationData d = batch.get(i);
                d.setTranslatedText(translated);
                toUpdate.add(d);
            }
        }

        if (!toUpdate.isEmpty()) {
            translationGateway.updateBatchTranslatedText(toUpdate);
            translationCache.invalidateBatch(toUpdate);
        }

        return toUpdate.size();
    }
}
