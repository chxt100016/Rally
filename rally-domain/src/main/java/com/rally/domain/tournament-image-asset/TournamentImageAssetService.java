package com.rally.domain.content.imageasset;

import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 生成赛事主图与轻量背景图，并按固定对象键依次覆盖保存。
 */
@Service
public class TournamentImageAssetService {

    private static final float MAIN_IMAGE_JPEG_QUALITY = 0.75F;
    private static final int BACKGROUND_TARGET_KB = 50;
    private static final String KEY_PREFIX = "tournament/";

    private final TournamentImageCodec imageCodec;
    private final TournamentImageStorage imageStorage;

    public TournamentImageAssetService(TournamentImageCodec imageCodec,
                                       TournamentImageStorage imageStorage) {
        this.imageCodec = imageCodec;
        this.imageStorage = imageStorage;
    }

    /**
     * 生成并保存一届赛事的主图和背景图。
     * 外部编码或存储异常均收敛为契约中的失败结论，不向上抛业务异常。
     */
    public TournamentImageAssetResult generate(String tournamentId, byte[] sourceImage) {
        if (tournamentId == null || tournamentId.isEmpty()
                || sourceImage == null || sourceImage.length == 0) {
            return TournamentImageAssetResult.failedBeforeSave();
        }

        byte[] original = Arrays.copyOf(sourceImage, sourceImage.length);
        try {
            if (!imageCodec.canDecode(Arrays.copyOf(original, original.length))) {
                return TournamentImageAssetResult.failedBeforeSave();
            }
        } catch (Exception e) {
            return TournamentImageAssetResult.failedBeforeSave();
        }

        String imageKey = KEY_PREFIX + tournamentId + ".jpg";
        String backgroundKey = KEY_PREFIX + tournamentId + "_background.jpg";

        // R1/R2：主图直接从原图独立编码，禁止复用后续背景图或其他有损结果。
        try {
            byte[] mainImage = imageCodec.encodeJpeg(
                    Arrays.copyOf(original, original.length), MAIN_IMAGE_JPEG_QUALITY);
            if (mainImage == null || mainImage.length == 0) {
                return TournamentImageAssetResult.failedBeforeSave();
            }
            // R3/R4/R5：固定键直接覆盖；主图必须先于背景图保存，且不做并发仲裁。
            imageStorage.overwrite(imageKey, mainImage);
        } catch (Exception e) {
            return TournamentImageAssetResult.failedBeforeSave();
        }

        // R1/R2：背景图再次直接使用原图，以 50KB 为目标；失败时保留已保存主图。
        try {
            byte[] backgroundImage = imageCodec.compressJpeg(
                    Arrays.copyOf(original, original.length), BACKGROUND_TARGET_KB);
            if (backgroundImage == null || backgroundImage.length == 0) {
                return TournamentImageAssetResult.failedAfterMainSave(imageKey);
            }
            imageStorage.overwrite(backgroundKey, backgroundImage);
        } catch (Exception e) {
            return TournamentImageAssetResult.failedAfterMainSave(imageKey);
        }

        return TournamentImageAssetResult.success(imageKey, backgroundKey);
    }
}
