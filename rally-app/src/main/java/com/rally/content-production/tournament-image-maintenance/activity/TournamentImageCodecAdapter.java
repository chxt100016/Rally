package com.rally.contentproduction.tournamentimagemaintenance.activity;

import com.rally.domain.content.imageasset.TournamentImageCodec;
import com.rally.domain.utils.ImageCompressor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

/** 使用项目既有图片工具落实赛事图片领域网关。 */
@Component
public class TournamentImageCodecAdapter implements TournamentImageCodec {

    @Override
    public boolean canDecode(byte[] sourceImage) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceImage)) {
            return ImageIO.read(input) != null;
        }
    }

    @Override
    public byte[] encodeJpeg(byte[] sourceImage, float quality) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceImage)) {
            return ImageCompressor.compressAsJpeg(input, quality);
        }
    }

    @Override
    public byte[] compressJpeg(byte[] sourceImage, int targetKb) throws Exception {
        try (ByteArrayInputStream input = new ByteArrayInputStream(sourceImage)) {
            return ImageCompressor.compressToJpeg(input, targetKb);
        }
    }
}
