package com.rally.web.tour;

import com.rally.contentproduction.tournamentimagemaintenance.activity.BindTournamentImagesActivity;
import com.rally.contentproduction.tournamentimagemaintenance.activity.GenerateTournamentImagesActivity;
import com.rally.contentproduction.tournamentimagemaintenance.activity.GenerateTournamentImagesResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/tour/upload")
public class TourUploadController {

    private final GenerateTournamentImagesActivity generateTournamentImagesActivity;
    private final BindTournamentImagesActivity bindTournamentImagesActivity;

    @PostMapping("/tournament")
    public Map<String, String> tournament(@RequestParam("file") MultipartFile file, @RequestParam("tournamentId") String tournamentId) throws Exception {
        GenerateTournamentImagesResult result = generateTournamentImagesActivity.execute(tournamentId, file);
        bindTournamentImagesActivity.execute(
                tournamentId, result.imageKey(), result.backgroundKey());
        // 生成可直接在线上执行的 UPDATE 语句，方便手动同步图片路径
        String sql = String.format("UPDATE tour_tournament SET image_path = '%s', background_path = '%s' WHERE tournament_id = '%s';", result.imageKey(), result.backgroundKey(), tournamentId);
        return Map.of("imageKey", result.imageKey(), "backgroundKey", result.backgroundKey(), "sql", sql
        );
    }
}
