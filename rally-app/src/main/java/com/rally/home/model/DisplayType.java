package com.rally.home.model;

import lombok.Getter;

@Getter
public enum DisplayType {
    MEETUP("约球活动"),
    TOUR_MATCH("赛事"),
    POSTER_CARD("海报卡片"),
    NEWS_TIMELINE("资讯时间线");

    private final String description;

    DisplayType(String description) {
        this.description = description;
    }
}
