package com.rally.domain.utils;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * 用法：修改 SOURCE_DIR、OUTPUT_DIR、TARGET_KB 后直接运行 testCompressAll()。
 */
public class ImageCompressorTest {

    // ===== 配置区 =====
    private static final String SOURCE_DIR = "/Users/mac/Downloads/done";   // 源图片文件夹
    private static final String OUTPUT_DIR = "/Users/mac/Downloads/tournament";  // 压缩后输出文件夹
    private static final int TARGET_KB  = 10;                   // 目标大小（KB）
    // ==================

    private static final List<String> SUPPORTED_EXTS = Arrays.asList("jpg", "jpeg", "png", "bmp", "gif");

    @Test
    public void testCompressAll() throws IOException {
        File srcDir = new File(SOURCE_DIR);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            System.out.println("源目录不存在，跳过：" + SOURCE_DIR);
            return;
        }

        File outDir = new File(OUTPUT_DIR);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        File[] files = srcDir.listFiles(f -> f.isFile() && isSupportedImage(f.getName()));
        if (files == null || files.length == 0) {
            System.out.println("源目录中没有支持的图片文件");
            return;
        }

        int success = 0, skip = 0;
        for (File file : files) {
            String ext = getExt(file.getName());
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] compressed = ImageCompressor.compress(fis, ext, TARGET_KB);
                Path outPath = Paths.get(OUTPUT_DIR, file.getName());
                try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                    fos.write(compressed);
                }
                long srcKb  = file.length() / 1024;
                long dstKb  = compressed.length / 1024;
                System.out.printf("%-40s %6d KB -> %6d KB%n", file.getName(), srcKb, dstKb);
                success++;
            } catch (IOException e) {
                System.out.println("压缩失败：" + file.getName() + " - " + e.getMessage());
                skip++;
            }
        }
        System.out.printf("%n完成：成功 %d 张，失败 %d 张，目标大小 %d KB%n", success, skip, TARGET_KB);
    }

    private static boolean isSupportedImage(String name) {
        return SUPPORTED_EXTS.contains(getExt(name));
    }

    private static String getExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
