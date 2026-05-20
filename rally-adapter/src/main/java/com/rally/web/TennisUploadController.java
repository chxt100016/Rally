package com.rally.web;

import com.rally.tennis.TennisUploadAppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/tennis/upload")
public class TennisUploadController {

    @Resource
    private TennisUploadAppService tennisUploadAppService;

    @PostMapping("/tournament")
    public Map<String, String> tournament(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tournamentId") String tournamentId) throws Exception {
        TennisUploadAppService.TournamentImageResult result =
                tennisUploadAppService.uploadTournamentImage(file, tournamentId);
        return Map.of(
                "imageKey", result.imageKey(),
                "backgroundKey", result.backgroundKey()
        );
    }
}
