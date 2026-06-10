package com.rally.db.court.repository;

import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 球场 Repository（门面层）
 */
@Repository
@RequiredArgsConstructor
public class CourtRepository {

    private final CourtService courtService;

    public void save(CourtPO po) {
        courtService.save(po);
    }

    public void updateById(CourtPO po) {
        courtService.updateById(po);
    }

    public CourtPO findByBizId(String bizId) {
        return courtService.lambdaQuery()
                .eq(CourtPO::getBizId, bizId)
                .one();
    }

    public List<CourtPO> findByCityCode(String cityCode) {
        return courtService.lambdaQuery()
                .eq(CourtPO::getCityCode, cityCode)
                .list();
    }

    public List<CourtPO> fuzzySearchByName(String cityCode, String keyword) {
        return courtService.lambdaQuery()
                .eq(CourtPO::getCityCode, cityCode)
                .like(keyword != null && !keyword.isBlank(), CourtPO::getName, keyword)
                .list();
    }
}
