package com.rally.web.content;

import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tennis.TennisContentAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping(value = "/seeds", produces = "text/plain;charset=UTF-8")
    public String getSeedList(
            @RequestParam("tournamentIds") List<String> tournamentIds,
            @RequestParam(value = "lang", defaultValue = "ZH_CN") TranslationLanguageEnum lang) {
        return tennisContentAppService.generateSeedListContent(tournamentIds, lang);
    }
}
