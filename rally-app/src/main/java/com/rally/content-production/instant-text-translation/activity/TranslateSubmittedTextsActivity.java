package com.rally.contentproduction.instanttexttranslation.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.translation.gateway.TranslationClient;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 业务活动 translate-submitted-texts：整批翻译当次提交的临时网球文本。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateSubmittedTextsActivity {

    private static final Set<TranslationEntityTypeEnum> SUPPORTED_ENTITY_TYPES = EnumSet.of(
            TranslationEntityTypeEnum.COURT,
            TranslationEntityTypeEnum.PLAYER,
            TranslationEntityTypeEnum.TOURNAMENT,
            TranslationEntityTypeEnum.SURFACE,
            TranslationEntityTypeEnum.CITY);

    private static final Set<TranslationLanguageEnum> SUPPORTED_LANGUAGES = EnumSet.of(
            TranslationLanguageEnum.ZH_CN,
            TranslationLanguageEnum.ZH_TW,
            TranslationLanguageEnum.EN,
            TranslationLanguageEnum.JA,
            TranslationLanguageEnum.KO);

    private final TranslationClient translationClient;

    public List<String> execute(List<TranslationData> tasks) {
        validate(tasks);
        if (tasks.isEmpty()) {
            return List.of();
        }

        try {
            // A2/A3 由既有翻译客户端集中构造网球语境提示词，并把原顺序整批一次提交。
            List<String> translations = translationClient.translate(tasks);

            // A4 任何无效整批结果均不交付部分译文；数量一致时原样保留空白与顺序。
            if (translations == null || translations.size() != tasks.size()) {
                return null;
            }
            return translations;
        } catch (RuntimeException exception) {
            log.error("整批临时文本翻译失败，丢弃本批结果", exception);
            return null;
        }
    }

    private void validate(List<TranslationData> tasks) {
        // A1 空列表是有效请求；缺少任务、条目、实体类型或目标语言则整批拒绝。
        if (tasks == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR);
        }
        for (TranslationData task : tasks) {
            if (task == null
                    || !SUPPORTED_ENTITY_TYPES.contains(task.getEntityType())
                    || !SUPPORTED_LANGUAGES.contains(task.getLanguage())) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR);
            }
        }
    }
}
