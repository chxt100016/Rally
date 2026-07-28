package com.rally.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rally.db.court.convert.CourtConvertMapper;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.service.CourtService;
import com.rally.domain.court.cache.CourtCache;
import com.rally.domain.court.model.CourtData;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 球场内存缓存（Caffeine），按 bizId 缓存，缺失回源 DB。
 */
@Component
public class CaffeineCourtCache implements CourtCache {

    private final CourtService courtService;
    private final Cache<String, CourtData> cache;

    public CaffeineCourtCache(CourtService courtService) {
        this.courtService = courtService;
        this.cache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public CourtData getByBizId(String bizId) {
        if (bizId == null || bizId.isBlank()) {
            return null;
        }
        return cache.get(bizId, this::loadByBizId);
    }

    @Override
    public void invalidate(String bizId) {
        if (bizId != null) {
            cache.invalidate(bizId);
        }
    }

    private CourtData loadByBizId(String bizId) {
        CourtPO po = courtService.lambdaQuery().eq(CourtPO::getBizId, bizId).one();
        return po == null ? null : CourtConvertMapper.INSTANCE.toCourtData(po);
    }
}
