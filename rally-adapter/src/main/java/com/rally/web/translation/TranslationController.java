package com.rally.web.translation;

import com.rally.domain.tennis.model.Result;
import com.rally.translation.TranslationBatchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/translation")
public class TranslationController {

    @Resource
    private TranslationBatchService translationBatchService;

    /**
     * 手动触发批量翻译，返回本次成功翻译的条数
     */
    @PostMapping("/batch")
    public Result<Integer> batchTranslate() {
        int count = translationBatchService.executeBatchTranslation();
        return Result.ok(count);
    }

    @PostMapping("/translate")
    public Result<String> translate() {
        return null;
    }
}
