package com.rally.web.translation;

import com.rally.domain.tennis.model.Result;
import com.rally.domain.translation.TranslationPromptBuilder;
import com.rally.domain.translation.model.TranslationData;
import com.rally.translation.TranslationAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/translation")
public class TranslationController {

    @Resource
    private TranslationAppService translationAppService;

    /**
     * 手动触发批量翻译，返回本次成功翻译的条数
     */
    @PostMapping("/batch")
    public Result<Integer> batchTranslate() {
        int count = translationAppService.batch();
        return Result.ok(count);
    }

    @PostMapping("/translate")
    public List<String> translate(@RequestBody List<TranslationData> data) {
        return this.translationAppService.process(data);
    }

    /**
     * 预览将要发送给 AI 的完整 prompt，便于调试提示词效果。
     * 返回纯文本：system prompt 与 user content 之间用分隔线隔开。
     */
    @GetMapping(value = "/prompt-preview", produces = "text/plain;charset=UTF-8")
    public String promptPreview() {
        List<TranslationData> pending = translationAppService.findAllPending();
        String systemPrompt = TranslationPromptBuilder.buildSystemPrompt(pending);
        String userContent = TranslationPromptBuilder.buildUserContentWithId(pending);
        return "=== SYSTEM ===\n" + systemPrompt
                + "\n\n=== USER ===\n" + userContent;
    }
}

