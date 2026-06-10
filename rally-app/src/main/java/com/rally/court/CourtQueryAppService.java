package com.rally.court;

import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtQueryCmd;
import com.rally.domain.court.service.CourtQueryDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 球场查询应用服务
 */
@Service
@RequiredArgsConstructor
public class CourtQueryAppService {

    private final CourtQueryDomainService courtQueryDomainService;

    /**
     * 模糊搜索球场
     */
    public List<CourtDTO> search(CourtQueryCmd cmd) {
        return courtQueryDomainService.searchByName(cmd);
    }
}
