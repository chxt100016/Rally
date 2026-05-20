package com.rally.domain.utils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * 图片压缩工具，支持将图片压缩到指定大小（KB）。
 * 策略：先降质量，质量降到底后等比缩小尺寸，直到满足目标大小。
 */
public class ImageCompressor {

    private static final float QUALITY_STEP = 0.05f;
    private static final float MIN_QUALITY = 0.1f;
    private static final float SCALE_STEP = 0.1f;

    public static byte[] compress(InputStream inputStream, String format, int targetKb) throws IOException {
        long targetBytes = (long) targetKb * 1024;
        BufferedImage original = ImageIO.read(inputStream);
        if (original == null) {
            throw new IOException("无法读取图片，请检查格式是否支持");
        }

        String fmt = format.toLowerCase().replace("jpeg", "jpg");
        if ("png".equals(fmt)) {
            fmt = "jpg";
            original = toRgb(original);
        }

        byte[] result = toBytes(original, fmt, 1.0f);
        if (result.length <= targetBytes) {
            return result;
        }

        // 第一阶段：降质量
        float quality = 1.0f - QUALITY_STEP;
        while (quality >= MIN_QUALITY) {
            result = toBytes(original, fmt, quality);
            if (result.length <= targetBytes) {
                return result;
            }
            quality -= QUALITY_STEP;
        }

        // 第二阶段：等比缩小尺寸
        float scale = 1.0f - SCALE_STEP;
        while (scale > 0.01f) {
            int w = Math.max(1, (int) (original.getWidth() * scale));
            int h = Math.max(1, (int) (original.getHeight() * scale));
            BufferedImage scaled = scaleImage(original, w, h);
            result = toBytes(scaled, fmt, MIN_QUALITY);
            if (result.length <= targetBytes) {
                return result;
            }
            scale -= SCALE_STEP;
        }

        return result;
    }

    private static byte[] toBytes(BufferedImage image, String format, float quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if ("jpg".equals(format)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) throw new IOException("找不到 JPEG 编码器");
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            ImageIO.write(image, format, out);
        }
        return out.toByteArray();
    }

    private static BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage toRgb(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rgb;
    }
}
