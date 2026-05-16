package com.rally.web.translation;

import com.rally.domain.tennis.model.Result;
import com.rally.domain.translation.model.TranslationData;
import com.rally.translation.TranslationAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
