package com.rally.domain.tour.draw;

/** 来源原始的签表规模与总轮数；两者独立可空，不做数学一致性推断。 */
public record TourDrawStructure(Integer size, Integer totalRounds) {
}
