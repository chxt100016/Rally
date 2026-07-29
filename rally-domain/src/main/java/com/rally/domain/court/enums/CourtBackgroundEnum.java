package com.rally.domain.court.enums;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 约球卡片背景样式标识解析。
 * <p>
 * 前端按此标识使用 CSS 渐变和 SVG 线描图标渲染背景。
 * 室外格式：{surface}-{时段}-{天气}，如 hard-day-clear、clay-night-rain；
 * 室内格式：indoor-{surface}，如 indoor-hard。
 * 降级：surface→HARD、venue→OUTDOOR、weather→CLEAR；startTime 必传。
 */
public final class CourtBackgroundEnum {

    private CourtBackgroundEnum() {
    }

    /** 时段：黄昏 17:00-19:00，夜晚 19:00-次日 6:00，其余白天 */
    private enum TimeSlot {
        DAY("day"), DUSK("dusk"), NIGHT("night");
        final String code;
        TimeSlot(String code) {
            this.code = code;
        }
    }

    /**
     * 解析供前端使用的背景样式标识。
     * @param surface 场地材质，null 降级 HARD
     * @param venue 室内外，null 降级 OUTDOOR
     * @param startTime 开球时间，必传，用于确定时段
     * @param weather 天气，null 降级 CLEAR
     * @return 背景样式标识
     */
    public static String resolveStyle(CourtSurfaceEnum surface, CourtEnvironmentEnum venue, LocalDateTime startTime, WeatherEnum weather) {
        CourtSurfaceEnum resolvedSurface = surface != null ? surface : CourtSurfaceEnum.HARD;
        CourtEnvironmentEnum resolvedVenue = venue != null ? venue : CourtEnvironmentEnum.OUTDOOR;
        WeatherEnum resolvedWeather = weather != null ? weather : WeatherEnum.CLEAR;
        String surfaceCode = resolvedSurface.name().toLowerCase();
        if (resolvedVenue == CourtEnvironmentEnum.INDOOR) {
            return "indoor-" + surfaceCode;
        }
        return surfaceCode + "-" + resolveSlot(startTime).code + "-" + resolvedWeather.name().toLowerCase();
    }

    private static TimeSlot resolveSlot(LocalDateTime startTime) {
        LocalTime time = startTime.toLocalTime();
        if (!time.isBefore(LocalTime.of(17, 0)) && time.isBefore(LocalTime.of(19, 0))) {
            return TimeSlot.DUSK;
        }
        if (!time.isBefore(LocalTime.of(19, 0)) || time.isBefore(LocalTime.of(6, 0))) {
            return TimeSlot.NIGHT;
        }
        return TimeSlot.DAY;
    }
}
