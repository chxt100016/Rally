package com.rally.domain.translation;

import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 负责将翻译任务列表组装为发送给 AI 的完整 prompt（system + user 两段）。
 * 提示词工程集中在此处，便于独立调试和迭代。
 */
public class TranslationPromptBuilder {

    private static final String BASE_SYSTEM_PROMPT =
            "你是一名专业的网球领域翻译专家，熟悉网球术语、球员姓名、赛事名称及场地类型等专有名词的标准译法。\n" +
            "任务规则：\n" +
            "1. 每行是一个独立翻译任务，格式为：文案:AA;实体:BB;翻译后语言:CC\n" +
            "   - 文案：需要翻译的原始内容\n" +
            "   - 实体：文案所属的业务实体（如 player/tournament/match/surface 等），用于辅助理解上下文\n" +
            "   - 翻译后语言：目标语言（如 zh-CN/en/fr 等）\n" +
            "2. 按原始顺序逐行返回翻译结果，一行输入对应一行输出，行数严格一致\n" +
            "3. 只输出翻译文案本身，不加序号、标点、解释或任何多余内容\n" +
            "4. 专有名词（球员姓名、赛事名称）优先使用业界通用译名";

    public static String buildSystemPrompt(List<TranslationData> tasks) {
        String hints = buildEntityHints(tasks);
        if (hints.isEmpty()) {
            return BASE_SYSTEM_PROMPT;
        }
        return BASE_SYSTEM_PROMPT + "\n5. 各实体类型的额外翻译说明（未列出的实体类型无需特殊处理）：\n" + hints;
    }

    public static String buildUserContent(List<TranslationData> tasks) {
        return tasks.stream()
                .map(TranslationPromptBuilder::toTaskLine)
                .collect(Collectors.joining("\n"));
    }

    private static String toTaskLine(TranslationData data) {
        return "文案:" + data.getOriginalText()
                + ";实体:" + data.getEntityType().getChineseDesc()
                + ";翻译后语言:" + data.getLanguage().getChineseDesc();
    }

    public static String buildUserContentWithId(List<TranslationData> tasks) {
        return tasks.stream()
                .map(TranslationPromptBuilder::toTaskLineWithId)
                .collect(Collectors.joining("\n"));
    }

    private static String toTaskLineWithId(TranslationData data) {
        return "id:" + data.getId() + ";文案:" + data.getOriginalText()
                + ";实体:" + data.getEntityType().getChineseDesc()
                + ";翻译后语言:" + data.getLanguage().getChineseDesc();
    }

    /** 收集本批次涉及的、有 hint 的实体类型，拼成补充说明段落 */
    private static String buildEntityHints(List<TranslationData> tasks) {
        return tasks.stream()
                .map(TranslationData::getEntityType)
                .distinct()
                .filter(e -> e.getHint() != null)
                .map(e -> "   - " + e.getChineseDesc() + "：" + e.getHint())
                .collect(Collectors.joining("\n"));
    }

    /** 返回所有枚举中配置了 hint 的实体说明（用于文档/调试） */
    public static String allEntityHints() {
        return Arrays.stream(TranslationEntityTypeEnum.values())
                .filter(e -> e.getHint() != null)
                .map(e -> e.name() + "（" + e.getChineseDesc() + "）：" + e.getHint())
                .collect(Collectors.joining("\n"));
    }
}
