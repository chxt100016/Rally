package com.rally.client.deepseek;

import com.rally.domain.translation.TranslationPromptBuilder;
import com.rally.domain.translation.gateway.TranslationClient;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient implements TranslationClient {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Override
    public List<String> translate(List<TranslationData> tasks) {
        if (tasks == null || tasks.isEmpty()) return List.of();

        String systemPrompt = TranslationPromptBuilder.buildSystemPrompt(tasks);
        String userContent = TranslationPromptBuilder.buildUserContent(tasks);

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-v4-pro",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
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
