package com.rally.db.court.repository;

import com.rally.db.court.convert.CourtConvertMapper;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.service.CourtService;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 球场 Repository 实现
 */
@Slf4j
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
                updateById(existing.getId(), po);
                return;
            }
        }
        courtService.save(po);
    }

    /**
     * 逐字段显式改写。updateById 默认跳过 null 字段，会让「清空别名/标签」这类改写落不了库，
     * 因此这里用 set 明确写入，包括写成 null。
     * biz_id、source、source_id、meetup_count、create_time 不在改写范围内。
     */
    private void updateById(Long id, CourtPO po) {
        courtService.lambdaUpdate()
                .eq(CourtPO::getId, id)
                .set(CourtPO::getName, po.getName())
                .set(CourtPO::getAlias, po.getAlias())
                .set(CourtPO::getAddress, po.getAddress())
                .set(CourtPO::getLng, po.getLng())
                .set(CourtPO::getLat, po.getLat())
                .set(CourtPO::getCityCode, po.getCityCode())
                .set(CourtPO::getCityName, po.getCityName())
                .set(CourtPO::getDistrictCode, po.getDistrictCode())
                .set(CourtPO::getDistrictName, po.getDistrictName())
                .set(CourtPO::getRemark, po.getRemark())
                .set(CourtPO::getType, po.getType())
                .set(CourtPO::getSurface, po.getSurface())
                .set(CourtPO::getTags, po.getTags())
                .set(CourtPO::getExtData, po.getExtData())
                .set(CourtPO::getStatus, po.getStatus())
                .update();
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

    @Override
    public List<CourtData> findBySourceIds(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        List<CourtPO> poList = courtService.lambdaQuery()
                .in(CourtPO::getSourceId, sourceIds)
                .list();
        return MAPPER.toCourtDataList(poList);
    }

    @Override
    public int batchSave(List<CourtData> courts) {
        if (courts == null || courts.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (CourtData data : courts) {
            try {
                save(data);
                success++;
            } catch (Exception e) {
                log.warn("球场写入失败，跳过该条。bizId={} sourceId={}", data.getBizId(), data.getSourceId(), e);
            }
        }
        return success;
    }

    @Override
    public void batchIncrementMeetupCount(Map<String, Integer> courtIdCountMap) {
        courtService.batchIncrementMeetupCount(courtIdCountMap);
    }

    private CourtPO findPoByBizId(String bizId) {
        return courtService.lambdaQuery()
                .eq(CourtPO::getBizId, bizId)
                .one();
    }
}
