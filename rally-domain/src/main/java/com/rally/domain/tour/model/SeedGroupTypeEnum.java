package com.rally.domain.tour.model;

import lombok.Getter;

@Getter
public enum SeedGroupTypeEnum {

    ATP,
    WTA,
    OUT;

    public static SeedGroupTypeEnum fromSeed(SeedVO seed) {
        if (seed.getStatus() == SeedStatusEnum.ELIMINATED) return OUT;
        if ("ATP".equalsIgnoreCase(seed.getTour())) return ATP;
        return WTA;
    }
}
