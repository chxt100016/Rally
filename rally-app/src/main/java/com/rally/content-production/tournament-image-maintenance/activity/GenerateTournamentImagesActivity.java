package com.rally.contentproduction.tournamentimagemaintenance.activity;

import com.rally.domain.content.imageasset.TournamentImageAssetOutcome;
import com.rally.domain.content.imageasset.TournamentImageAssetResult;
import com.rally.domain.content.imageasset.TournamentImageAssetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 业务活动 generate-tournament-images：从一张原图生成并保存赛事主图与背景图。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateTournamentImagesActivity {

    private final TournamentImageAssetService tournamentImageAssetService;

    public GenerateTournamentImagesResult execute(String tournamentId, MultipartFile file) {
        // A1 赛事编号仅校验非空，后续保留调用方提交的原值生成稳定对象键。
        if (tournamentId == null || tournamentId.isEmpty() || file == null || file.isEmpty()) {
            throw systemError("赛事编号或原图为空", tournamentId, null);
        }

        byte[] sourceImage;
        try {
            sourceImage = file.getBytes();
        } catch (IOException exception) {
            throw systemError("读取赛事原图失败", tournamentId, exception);
        }
        if (sourceImage.length == 0) {
            throw systemError("赛事原图为空", tournamentId, null);
        }

        // A2/A3 聚合从同一原图独立生成两份 JPEG，并按主图、背景图顺序覆盖固定对象键。
        TournamentImageAssetResult result = tournamentImageAssetService.generate(tournamentId, sourceImage);
        if (result.getOutcome() != TournamentImageAssetOutcome.SUCCESS) {
            throw systemError("生成或保存赛事图片失败: " + result.getOutcome(), tournamentId, null);
        }

        // A4 只交付资源标识；赛事资料绑定和访问地址不属于本活动。
        return new GenerateTournamentImagesResult(result.getImageKey(), result.getBackgroundKey());
    }

    private IllegalStateException systemError(String message, String tournamentId, Exception cause) {
        if (cause == null) {
            log.error("{}: tournamentId={}", message, tournamentId);
            return new IllegalStateException(message);
        }
        log.error("{}: tournamentId={}", message, tournamentId, cause);
        return new IllegalStateException(message, cause);
    }
}
