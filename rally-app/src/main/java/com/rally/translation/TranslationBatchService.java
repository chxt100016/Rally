package com.rally.translation;

import com.rally.client.deepseek.DeepSeekClient;
import com.rally.domain.translation.gateway.TranslationGateway;
import com.rally.domain.translation.model.TranslationData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TranslationBatchService {

    @Resource
    private TranslationGateway translationGateway;

    @Resource
    private DeepSeekClient deepSeekClient;

    @Resource
    private TranslationService translationService;

    /** 每批次最多发送给 DeepSeek 的条数，避免单次请求过大 */
    private static final int BATCH_SIZE = 50;

    /**
     * 执行批量翻译：
     * 1. 查询所有 translatedText 为 null 的待翻译记录
     * 2. 分批调用 DeepSeek API
     * 3. 将翻译结果写回数据库并清除本地缓存
     *
     * @return 本次翻译成功的条数
     */
    public int executeBatchTranslation() {
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

    private int processBatch(List<TranslationData> batch) {
        List<String> tasks = batch.stream().map(this::buildTaskLine).toList();

        List<String> results = deepSeekClient.translate(tasks);
        if (results == null) {
            log.error("DeepSeek 翻译失败，跳过本批次 {} 条", batch.size());
            return 0;
        }

        if (results.size() != batch.size()) {
            log.error("DeepSeek 返回行数 {} 与输入行数 {} 不匹配，跳过本批次", results.size(), batch.size());
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
            translationService.invalidateCache(toUpdate);
        }

        return toUpdate.size();
    }

    /** 构造单行翻译任务：文案:AA;实体:BB;翻译后语言:CC */
    private String buildTaskLine(TranslationData data) {
        return "文案:" + data.getOriginalText()
                + ";实体:" + data.getEntityType().getChineseDesc()
                + ";翻译后语言:" + data.getLanguage().getChineseDesc();
    }
}
