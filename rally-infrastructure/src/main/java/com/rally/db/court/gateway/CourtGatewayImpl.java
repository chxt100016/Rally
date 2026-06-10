package com.rally.db.court.gateway;

import com.rally.db.court.convert.CourtConvertMapper;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.repository.CourtRepository;
import com.rally.domain.court.gateway.CourtGateway;
import com.rally.domain.court.model.CourtData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 球场网关实现
 */
@Component
@RequiredArgsConstructor
public class CourtGatewayImpl implements CourtGateway {

    private final CourtRepository courtRepository;

    @Override
    public void save(CourtData data) {
        CourtPO po = CourtConvertMapper.INSTANCE.toCourtPO(data);
        if (data.getBizId() != null) {
            CourtPO existing = courtRepository.findByBizId(data.getBizId());
            if (existing != null) {
                po.setId(existing.getId());
                courtRepository.updateById(po);
                return;
            }
        }
        courtRepository.save(po);
    }

    @Override
    public CourtData findByBizId(String bizId) {
        CourtPO po = courtRepository.findByBizId(bizId);
        return po == null ? null : CourtConvertMapper.INSTANCE.toCourtData(po);
    }

    @Override
    public List<CourtData> findByCityCode(String cityCode) {
        return CourtConvertMapper.INSTANCE.toCourtDataList(courtRepository.findByCityCode(cityCode));
    }

    @Override
    public List<CourtData> fuzzySearchByName(String cityCode, String keyword) {
        return CourtConvertMapper.INSTANCE.toCourtDataList(courtRepository.fuzzySearchByName(cityCode, keyword));
    }
}
