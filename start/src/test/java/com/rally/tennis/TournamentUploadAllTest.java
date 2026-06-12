package com.rally.tennis;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(classes = com.rally.Application.class)
public class TournamentUploadAllTest {

    private static final String SOURCE_DIR = "/Users/mac/Downloads/done";

    private static final List<String> SUPPORTED_EXTS = Arrays.asList("jpg", "jpeg", "png", "bmp", "gif", "webp");

    @Resource
    private TennisUploadAppService tennisUploadAppService;

    @Test
    public void uploadAll() {
        File srcDir = new File(SOURCE_DIR);
        if (!srcDir.exists() || !srcDir.isDirectory()) {
            System.out.println("源目录不存在，跳过：" + SOURCE_DIR);
            return;
        }
        File[] files = srcDir.listFiles(f -> f.isFile() && isSupportedImage(f.getName()));
        if (files == null || files.length == 0) {
            System.out.println("源目录中没有支持的图片文件");
            return;
        }
        int success = 0, fail = 0;
        for (File file : files) {
            String tournamentId = file.getName().substring(0, file.getName().lastIndexOf('.'));
            System.out.printf("开始上传: %s (tournamentId=%s, size=%dKB)%n",
                    file.getName(), tournamentId, file.length() / 1024);
            try (FileInputStream fis = new FileInputStream(file)) {
                String contentType = Files.probeContentType(file.toPath());
                MultipartFile multipartFile = new MockMultipartFile(
                        "file", file.getName(), contentType, fis);
                System.out.printf("  MultipartFile: name=%s, size=%d, contentType=%s%n",
                        multipartFile.getName(), multipartFile.getSize(), multipartFile.getContentType());
                TennisUploadAppService.TournamentImageResult result =
                        tennisUploadAppService.uploadTournamentImage(multipartFile, tournamentId);
                System.out.printf("  成功 -> image: %s, bg: %s%n", result.imageKey(), result.backgroundKey());
                success++;
            } catch (Exception e) {
                System.out.printf("  失败: %s - %s%n", e.getClass().getSimpleName(), e.getMessage());
                e.printStackTrace(System.out);
                fail++;
            }
        }
        System.out.printf("%n上传完成：成功 %d，失败 %d%n", success, fail);
    }

    private static boolean isSupportedImage(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1).toLowerCase();
        return SUPPORTED_EXTS.contains(ext);
    }
}
