package com.rally.client.deepseek;

import com.rally.db.translation.convert.TranslationTaskConvertMapper;
import com.rally.domain.translation.gateway.TranslationClient;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeepSeekClient implements TranslationClient {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    // 系统角色定义：明确翻译场景、输入格式、输出规范，减少模型自由发挥
    private static final String SYSTEM_PROMPT =
            "你是一名专业的网球领域翻译专家，熟悉网球术语、球员姓名、赛事名称及场地类型等专有名词的标准译法。\n" +
            "任务规则：\n" +
            "1. 每行是一个独立翻译任务，格式为：文案:AA;实体:BB;翻译后语言:CC\n" +
            "   - 文案：需要翻译的原始内容\n" +
            "   - 实体：文案所属的业务实体（如 player/tournament/match/surface 等），用于辅助理解上下文\n" +
            "   - 翻译后语言：目标语言（如 zh-CN/en/fr 等）\n" +
            "2. 按原始顺序逐行返回翻译结果，一行输入对应一行输出，行数严格一致\n" +
            "3. 只输出翻译文案本身，不加序号、标点、解释或任何多余内容\n" +
            "4. 专有名词（球员姓名、赛事名称）优先使用业界通用译名";

    @Override
    public List<String> translate(List<TranslationData> tasks) {
        if (tasks == null || tasks.isEmpty()) return List.of();

        List<String> taskLines = tasks.stream()
                .map(TranslationTaskConvertMapper.INSTANCE::toTaskLine)
                .collect(Collectors.toList());

        String userContent = taskLines.stream()
                .map(String::trim)
                .collect(Collectors.joining("\n"));

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-v4-pro",
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent)
                ),
                // thinking 模式：让模型先推理再输出，提升专有名词译名准确率
                "thinking", Map.of("type", "enabled"),
                "reasoning_effort", "high",
                "max_tokens", 4096,
                "temperature", 1
        );

        try {
            DeepSeekResponse response = Http.uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .jsonHeader()
                    .entity(requestBody)
                    .doPost()
                    .result(DeepSeekResponse.class);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                log.error("DeepSeek 返回结果为空");
                return null;
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            // 严格按行分割，不过滤空行，保证与输入 tasks 行数一一对应
            List<String> results = List.of(content.split("\n"));
            if (results.size() != tasks.size()) {
                log.error("DeepSeek 返回行数 {} 与输入任务数 {} 不一致", results.size(), tasks.size());
                return null;
            }
            return results;
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败", e);
            return null;
        }
    }

    @Data
    public static class DeepSeekResponse {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private Message message;
        }

        @Data
        public static class Message {
            private String content;
        }
    }
}
