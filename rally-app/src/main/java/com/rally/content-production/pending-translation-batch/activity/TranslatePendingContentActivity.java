package com.rally.contentproduction.pendingtranslationbatch.activity;

import com.rally.domain.content.translationentry.SaveTranslationTextCmd;
import com.rally.domain.content.translationentry.TranslationEntry;
import com.rally.domain.content.translationentry.TranslationEntryState;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.gateway.TranslationClient;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务活动 translate-pending-content：分批翻译请求开始时的全部待译条目。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslatePendingContentActivity {

    private static final int BATCH_SIZE = 50;

    private final TranslationRepository translationRepository;
    private final TranslationClient translationClient;
    private final TranslationCache translationCache;

    public int execute() {
        // A1 一次性复制当前已提交的待译记录，后续新增记录不进入本次处理。
        List<TranslationData> pending = new ArrayList<>(translationRepository.findAllPending());
        if (pending.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        // A2 保留仓储返回顺序，每 50 条一批，不会产生空批次。
        for (int start = 0; start < pending.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, pending.size());
            List<TranslationData> batch = pending.subList(start, end);
            successCount += translateAndSaveBatch(batch);
        }
        return successCount;
    }

    private int translateAndSaveBatch(List<TranslationData> batch) {
        List<String> translations;
        try {
            // A3 整批交给既有 DeepSeek 客户端；失败或数量不符时本批跳过。
            translations = translationClient.translate(batch);
        } catch (RuntimeException exception) {
            log.error("批量翻译失败，跳过本批次 {} 条", batch.size(), exception);
            return 0;
        }
        if (translations == null || translations.size() != batch.size()) {
            log.error("翻译结果无效，跳过本批次 {} 条", batch.size());
            return 0;
        }

        List<TranslationData> updates = new ArrayList<>();
        for (int index = 0; index < batch.size(); index++) {
            String translatedText = translations.get(index);
            if (translatedText == null || translatedText.strip().isEmpty()) {
                continue;
            }

            TranslationData pending = batch.get(index);
            TranslationEntry entry = TranslationEntry.restore(toState(pending));
            entry.saveTranslation(new SaveTranslationTextCmd(pending.getId(), translatedText));
            updates.add(toData(entry.state()));
        }

        // A4 空白译文保持待译；非空译文仅按 ID 回写，每批成功后失效缓存。
        if (!updates.isEmpty()) {
            translationRepository.updateBatchTranslatedText(updates);
            translationCache.invalidate();
        }
        return updates.size();
    }

    private TranslationEntryState toState(TranslationData data) {
        return new TranslationEntryState(
                data.getId(),
                data.getEntityType(),
                data.getOriginalText(),
                data.getLanguage(),
                data.getTranslatedText(),
                null,
                null);
    }

    private TranslationData toData(TranslationEntryState state) {
        return new TranslationData()
                .setId(state.getId())
                .setTranslatedText(state.getTranslatedText());
    }
}
