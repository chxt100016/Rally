package com.rally.domain.utils;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImageCompressorJpegTest {

    @Test
    public void compressAsJpeg_convertsPngAndAppliesJpegEncoding() throws Exception {
        byte[] png = createPng();

        byte[] result = ImageCompressor.compressAsJpeg(new ByteArrayInputStream(png), 0.75f);

        assertTrue(result[0] == (byte) 0xFF && result[1] == (byte) 0xD8);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(80, image.getWidth());
        assertEquals(60, image.getHeight());
    }

    @Test
    public void compressToJpeg_convertsPngToJpeg() throws Exception {
        byte[] png = createPng();

        byte[] result = ImageCompressor.compressToJpeg(new ByteArrayInputStream(png), 50);

        assertTrue(result[0] == (byte) 0xFF && result[1] == (byte) 0xD8);
        assertTrue(result.length <= 50 * 1024);
    }

    private byte[] createPng() throws Exception {
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(255, 0, 0, 128));
        graphics.fillRect(0, 0, 80, 60);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
