package com.rally.court;

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
        return courtQueryDomainService.getAllByCityCode(cmd);
    }

    public List<CourtDTO> search(CourtSearchCmd cmd) {
        return courtQueryDomainService.searchByName(cmd);
    }
}
