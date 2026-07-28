package com.rally.domain.court.enums;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 约球卡片底图 key 解析。
 * <p>
 * key 前缀 default/，命名规则 {surface}-{时段}[-天气].jpg（室内为 indoor-{surface}.jpg）。
 * 只有室外硬地做全套时段+天气；红土/草地各只有一张白天兜底；室内各一张。
 * 降级：surface→HARD、venue→OUTDOOR、weather→CLEAR；startTime 必传，定时段。
 */
public final class CourtBackgroundEnum {

    private static final String PREFIX = "default/";
    private static final String SUFFIX = ".jpg";

    private CourtBackgroundEnum() {
    }

    /** 时段：黄昏 17:00-19:00，夜晚 19:00-次日6:00，其余白天 */
    private enum TimeSlot {
        DAY("day"), DUSK("dusk"), NIGHT("night");
        final String code;
        TimeSlot(String code) {
            this.code = code;
        }
    }

    /**
     * 解析底图对象存储 key（只返回 key，不含域名/签名）。
     * @param surface   场地材质，null 降级 HARD
     * @param venue     室内外，null 降级 OUTDOOR
     * @param startTime 开球时间，必传，用于定时段
     * @param weather   天气，null 降级 CLEAR（且仅室外硬地区分天气）
     */
    public static String resolveKey(CourtSurfaceEnum surface, CourtEnvironmentEnum venue, LocalDateTime startTime, WeatherEnum weather) {
        CourtSurfaceEnum s = surface != null ? surface : CourtSurfaceEnum.HARD;
        CourtEnvironmentEnum v = venue != null ? venue : CourtEnvironmentEnum.OUTDOOR;
        WeatherEnum w = weather != null ? weather : WeatherEnum.CLEAR;
        String surfaceCode = s.name().toLowerCase();

        // 室内：忽略时段与天气
        if (v == CourtEnvironmentEnum.INDOOR) {
            return PREFIX + "indoor-" + surfaceCode + SUFFIX;
        }

        // 室外红土/草地：只有白天兜底一张
        if (s != CourtSurfaceEnum.HARD) {
            return PREFIX + surfaceCode + "-day" + SUFFIX;
        }

        // 室外硬地：时段 + 天气（仅雨天有独立图，晴/阴共用无天气后缀的白天图除外）
        TimeSlot slot = resolveSlot(startTime);
        String name = surfaceCode + "-" + slot.code + weatherSuffix(slot, w);
        return PREFIX + name + SUFFIX;
    }

    /**
     * 天气后缀：白天区分 晴(无后缀)/阴(-cloudy)/雨(-rain)；黄昏/夜晚仅区分 雨(-rain)，其余无后缀。
     */
    private static String weatherSuffix(TimeSlot slot, WeatherEnum w) {
        if (slot == TimeSlot.DAY) {
            return switch (w) {
                case CLOUDY -> "-cloudy";
                case RAIN -> "-rain";
                case CLEAR -> "";
            };
        }
        return w == WeatherEnum.RAIN ? "-rain" : "";
    }

    private static TimeSlot resolveSlot(LocalDateTime startTime) {
        LocalTime t = startTime.toLocalTime();
        if (!t.isBefore(LocalTime.of(17, 0)) && t.isBefore(LocalTime.of(19, 0))) {
            return TimeSlot.DUSK;
        }
        if (!t.isBefore(LocalTime.of(19, 0)) || t.isBefore(LocalTime.of(6, 0))) {
            return TimeSlot.NIGHT;
        }
        return TimeSlot.DAY;
    }
}
