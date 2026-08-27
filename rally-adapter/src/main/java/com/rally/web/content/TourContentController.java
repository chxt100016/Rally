package com.rally.web.content;

import com.rally.contentproduction.tournamentimagemaintenance.activity.BindTournamentImagesActivity;
import com.rally.contentproduction.tournamentimagemaintenance.activity.GenerateTournamentImagesActivity;
import com.rally.contentproduction.tournamentimagemaintenance.activity.GenerateTournamentImagesResult;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tour.TourContentAppService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/content/tour")
public class TourContentController {


    @Resource
    private TourContentAppService tourContentAppService;

    @Resource
    private GenerateTournamentImagesActivity generateTournamentImagesActivity;

    @Resource
    private BindTournamentImagesActivity bindTournamentImagesActivity;

    @GetMapping(value = "/daily", produces = "text/plain;charset=UTF-8")
    public String getDailyContent() {
        return tourContentAppService.generateDailyContent();
    }

    @GetMapping(value = "/seeds", produces = "text/plain;charset=UTF-8")
    public String getSeedList(
            @RequestParam("tournamentIds") List<String> tournamentIds,
            @RequestParam(value = "lang", defaultValue = "ZH_CN") TranslationLanguageEnum lang) {
        return tourContentAppService.generateSeedListContent(tournamentIds, lang);
    }

    @GetMapping(value = "/poster/prompt", produces = "text/plain;charset=UTF-8")
    public String getPosterPrompt(@RequestParam("tournamentId") String tournamentId) {
        return tourContentAppService.generatePosterPrompt(tournamentId);
    }

    /**
     * 上传赛事主图和背景图。
     * 主图以 75% JPEG 质量保存为 tournament/{tournamentId}.jpg；
     * 背景图压缩至 50KB 以内，保存为 tournament/{tournamentId}_background.jpg。
     */
    @PostMapping("/image")
    public GenerateTournamentImagesResult uploadTournamentImage(
            @RequestParam("tournamentId") String tournamentId,
            @RequestParam("file") MultipartFile file) {
        GenerateTournamentImagesResult result = generateTournamentImagesActivity
                .execute(tournamentId, file);
        bindTournamentImagesActivity.execute(
                tournamentId, result.imageKey(), result.backgroundKey());
        return result;
    }
}
