package com.rally.web.content;

import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tennis.TennisContentAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/tennis/content")
public class TennisContentController {

    @Resource
    private TennisContentAppService tennisContentAppService;

    @GetMapping(value = "/daily", produces = "text/plain;charset=UTF-8")
    public String getDailyContent(
            @RequestParam("date") LocalDate date,
            @RequestParam(value = "lang", defaultValue = "ZH_CN") TranslationLanguageEnum lang) {
        return tennisContentAppService.generateDailyContent(date, lang);
    }
}
