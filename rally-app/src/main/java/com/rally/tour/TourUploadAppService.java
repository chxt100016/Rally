package com.rally.tour;

import com.qiniu.common.QiniuException;
import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.utils.ImageCompressor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class TourUploadAppService {

    private static final String DIR = "tournament";

    @Value("${upload.tournament.compress-kb:200}")
    private int compressKb;

    @Resource
    private QiniuClient qiniuClient;

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    public TournamentImageResult uploadTournamentImage(MultipartFile file, String tournamentId) throws IOException, QiniuException {
        byte[] originalBytes = file.getBytes();
        String format = resolveFormat(file.getContentType(), file.getOriginalFilename());
        byte[] compressedBytes = ImageCompressor.compress(new ByteArrayInputStream(originalBytes), format, compressKb);
        String imageKey = qiniuClient.uploadImage(originalBytes, DIR, tournamentId);
        String backgroundKey = qiniuClient.uploadImage(compressedBytes, DIR, tournamentId + "_background");
        tourTournamentRepository.updateImagePaths(tournamentId, imageKey, backgroundKey);
        return new TournamentImageResult(imageKey, backgroundKey);
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

    public record TournamentImageResult(String imageKey, String backgroundKey) {}
}
