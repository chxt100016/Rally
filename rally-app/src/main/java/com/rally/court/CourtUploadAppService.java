package com.rally.court;

import com.qiniu.common.QiniuException;
import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.utils.ImageCompressor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class CourtUploadAppService {

    private static final String DIR = "court";

    @Value("${upload.court.compress-kb:20}")
    private int compressKb;

    @Resource
    private QiniuClient qiniuClient;

    @Resource
    private CourtDomainService courtDomainService;

    public String uploadBackgroundImage(MultipartFile file, String courtId) throws IOException, QiniuException {
        byte[] originalBytes = file.getBytes();
        String format = resolveFormat(file.getContentType(), file.getOriginalFilename());
        byte[] compressedBytes = ImageCompressor.compress(new ByteArrayInputStream(originalBytes), format, compressKb);
        String key = qiniuClient.uploadImage(compressedBytes, DIR + "/" + courtId, "backgroundImage.jpeg");
        courtDomainService.updateBackgroundImage(courtId, key);
        return key;
    }

    private String resolveFormat(String contentType, String originalFilename) {
        if (contentType != null) {
            if (contentType.contains("png")) return "png";
            if (contentType.contains("gif")) return "gif";
            if (contentType.contains("webp")) return "webp";
        }
        if (originalFilename != null) {
            String lower = originalFilename.toLowerCase();
            if (lower.endsWith(".png")) return "png";
            if (lower.endsWith(".gif")) return "gif";
            if (lower.endsWith(".webp")) return "webp";
        }
        return "jpg";
    }
}
