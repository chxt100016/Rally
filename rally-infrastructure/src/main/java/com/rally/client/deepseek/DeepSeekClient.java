package com.rally.client.deepseek;

import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekClient {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    private static final String SYSTEM_PROMPT =
            "在网球场景下，帮我翻译下面的内容，我会给你文案和文案归属的实体还有需要翻译成的语言，" +
            "每行是一个需要翻译的任务，你返回就按每个任务的翻译文案为一行不要多余的话。";

    /**
     * 批量翻译，每行输入格式：文案:AA;实体:BB;翻译后语言:CC
     * 返回按行分割的翻译结果，顺序与输入一一对应；失败时返回 null
     */
    public List<String> translate(List<String> tasks) {
        if (tasks == null || tasks.isEmpty()) return List.of();

        String userContent = String.join("\n", tasks);
        Map<String, Object> requestBody = Map.of(
                "model", "deepseek-chat",
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.3
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
            // 按行分割，过滤空行
            return List.of(content.split("\n")).stream()
                    .filter(line -> !line.isBlank())
                    .toList();
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
