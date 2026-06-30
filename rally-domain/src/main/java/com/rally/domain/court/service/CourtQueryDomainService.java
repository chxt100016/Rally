package com.rally.domain.court.service;

import com.rally.domain.court.convert.CourtConvertMapper;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtListCmd;
import com.rally.domain.court.model.CourtSearchCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtQueryDomainService {

    private final CourtRepository courtRepository;

    public List<CourtDTO> getAllByCityCode(CourtListCmd cmd) {
        return CourtConvertMapper.INSTANCE.toDTOList(courtRepository.findByCityCode(cmd.getCityCode()));
    }

    public List<CourtDTO> searchByName(CourtSearchCmd cmd) {
        return CourtConvertMapper.INSTANCE.toDTOList(
                courtRepository.fuzzySearchByName(cmd.getCityCode(), cmd.getQuery()));
    }
}
