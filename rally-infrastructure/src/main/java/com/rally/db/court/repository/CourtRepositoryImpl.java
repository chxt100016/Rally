package com.rally.db.court.repository;

import com.rally.db.court.convert.CourtConvertMapper;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.service.CourtService;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 球场 Repository 实现
 */
@Component
@RequiredArgsConstructor
public class CourtRepositoryImpl implements CourtRepository {

    private final CourtService courtService;
    private static final CourtConvertMapper MAPPER = CourtConvertMapper.INSTANCE;

    @Override
    public void save(CourtData data) {
        CourtPO po = MAPPER.toCourtPO(data);
        if (data.getBizId() != null) {
            CourtPO existing = findPoByBizId(data.getBizId());
            if (existing != null) {
                po.setId(existing.getId());
                courtService.updateById(po);
                return;
            }
        }
        courtService.save(po);
    }

    @Override
    public CourtData findByBizId(String bizId) {
        CourtPO po = findPoByBizId(bizId);
        return po == null ? null : MAPPER.toCourtData(po);
    }

    @Override
    public List<CourtData> findByCityCode(String cityCode) {
        List<CourtPO> poList = courtService.lambdaQuery()
                .eq(CourtPO::getCityCode, cityCode)
                .eq(CourtPO::getStatus, CourtStatusEnum.ACTIVE)
                .list();
        return MAPPER.toCourtDataList(poList);
    }

    @Override
    public List<CourtData> fuzzySearchByName(String cityCode, String keyword) {
        List<CourtPO> poList = courtService.lambdaQuery()
                .eq(CourtPO::getCityCode, cityCode)
                .eq(CourtPO::getStatus, CourtStatusEnum.ACTIVE)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(CourtPO::getName, keyword).or().like(CourtPO::getAlias, keyword))
                .list();
        return MAPPER.toCourtDataList(poList);
    }

    private CourtPO findPoByBizId(String bizId) {
        return courtService.lambdaQuery()
                .eq(CourtPO::getBizId, bizId)
                .one();
    }
}
