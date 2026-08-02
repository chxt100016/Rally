package com.rally.tour;

import com.qiniu.common.QiniuException;
import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.utils.ImageCompressor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class TourUploadAppService {

    private static final String DIR = "tournament";
    private static final float IMAGE_JPEG_QUALITY = 0.75f;
    private static final int BACKGROUND_TARGET_KB = 50;

    @Resource
    private QiniuClient qiniuClient;

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    public TournamentImageResult uploadTournamentImage(MultipartFile file, String tournamentId) throws IOException, QiniuException {
        byte[] originalBytes = file.getBytes();
        byte[] imageBytes = ImageCompressor.compressAsJpeg(new ByteArrayInputStream(originalBytes), IMAGE_JPEG_QUALITY);
        byte[] backgroundBytes = ImageCompressor.compressToJpeg(new ByteArrayInputStream(originalBytes), BACKGROUND_TARGET_KB);
        String imageKey = qiniuClient.uploadImage(imageBytes, DIR, tournamentId + ".jpg");
        String backgroundKey = qiniuClient.uploadImage(backgroundBytes, DIR, tournamentId + "_background.jpg");
        tourTournamentRepository.updateImagePaths(tournamentId, imageKey, backgroundKey);
        return new TournamentImageResult(imageKey, backgroundKey);
    }

    public record TournamentImageResult(String imageKey, String backgroundKey) {}
}
