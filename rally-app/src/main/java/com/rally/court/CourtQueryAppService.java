package com.rally.court;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtListCmd;
import com.rally.domain.court.model.CourtSearchCmd;
import com.rally.domain.court.service.CourtQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtQueryAppService {

    private final CourtQueryDomainService courtQueryDomainService;

    public List<CourtDTO> getAll(CourtListCmd cmd) {
        List<CourtDTO> list = courtQueryDomainService.getAllByCityCode(cmd);
        list.forEach(this::fillBackgroundImageUrl);
        return list;
    }

    public List<CourtDTO> search(CourtSearchCmd cmd) {
        List<CourtDTO> list = courtQueryDomainService.searchByName(cmd);
        list.forEach(this::fillBackgroundImageUrl);
        return list;
    }

    private void fillBackgroundImageUrl(CourtDTO dto) {
        dto.setBackgroundImage(QiniuConfiguration.buildSignedUrl(dto.getBackgroundImage()));
    }
}
