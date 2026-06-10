package com.rally.domain.court.service;

import com.rally.domain.court.convert.CourtConvertMapper;
import com.rally.domain.court.gateway.CourtGateway;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtQueryCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 球场查询领域服务
 */
@Service
@RequiredArgsConstructor
public class CourtQueryDomainService {

    private final CourtGateway courtGateway;

    /**
     * 模糊搜索球场名称
     */
    public List<CourtDTO> searchByName(CourtQueryCmd cmd) {
        return CourtConvertMapper.INSTANCE.toDTOList(
                courtGateway.fuzzySearchByName(cmd.getCityCode(), cmd.getKeyword()));
    }
}
