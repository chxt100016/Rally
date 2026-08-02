package com.rally.domain.court.enums;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 约球卡片背景。
 * <p>
 * 前端按此标识使用 CSS 渐变和 SVG 线描图标渲染背景。
 * 室外格式：{surface}-{时段}-{天气}，如 hard-day-clear、clay-night-rain；
 * 室内格式：indoor-{surface}，如 indoor-hard。
 * 降级：surface→HARD、environment→OUTDOOR、weather→CLEAR。
 */
public enum CourtBackgroundEnum {

    INDOOR_HARD("indoor-hard"),
    INDOOR_CLAY("indoor-clay"),
    INDOOR_GRASS("indoor-grass"),

    HARD_DAY_CLEAR("hard-day-clear"),
    HARD_DAY_RAIN("hard-day-rain"),
    HARD_DUSK_CLEAR("hard-dusk-clear"),
    HARD_DUSK_RAIN("hard-dusk-rain"),
    HARD_NIGHT_CLEAR("hard-night-clear"),
    HARD_NIGHT_RAIN("hard-night-rain"),

    CLAY_DAY_CLEAR("clay-day-clear"),
    CLAY_DAY_RAIN("clay-day-rain"),
    CLAY_DUSK_CLEAR("clay-dusk-clear"),
    CLAY_DUSK_RAIN("clay-dusk-rain"),
    CLAY_NIGHT_CLEAR("clay-night-clear"),
    CLAY_NIGHT_RAIN("clay-night-rain"),

    GRASS_DAY_CLEAR("grass-day-clear"),
    GRASS_DAY_RAIN("grass-day-rain"),
    GRASS_DUSK_CLEAR("grass-dusk-clear"),
    GRASS_DUSK_RAIN("grass-dusk-rain"),
    GRASS_NIGHT_CLEAR("grass-night-clear"),
    GRASS_NIGHT_RAIN("grass-night-rain");

    private final String key;

    CourtBackgroundEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /** 时段：黄昏 17:00-19:00，夜晚 19:00-次日 6:00，其余白天 */
    private enum TimeSlot {
        DAY, DUSK, NIGHT
    }

    /**
     * 解析供前端使用的背景 key。
     *
     * @param surface 场地材质，null 降级 HARD
     * @param environment 室内外，null 降级 OUTDOOR
     * @param startTime 开球时间，为 null 时无法解析
     * @param weather 天气，null 降级 CLEAR
     * @return 背景 key，开球时间为 null 时返回 null
     */
    public static String resolveKey(CourtSurfaceEnum surface, CourtEnvironmentEnum environment,
                                    LocalDateTime startTime, WeatherEnum weather) {
        CourtBackgroundEnum background = resolve(surface, environment, startTime, weather);
        return background != null ? background.getKey() : null;
    }

    public static CourtBackgroundEnum resolve(CourtSurfaceEnum surface, CourtEnvironmentEnum environment,
                                               LocalDateTime startTime, WeatherEnum weather) {
        if (startTime == null) {
            return null;
        }
        CourtSurfaceEnum resolvedSurface = surface != null ? surface : CourtSurfaceEnum.HARD;
        CourtEnvironmentEnum resolvedEnvironment = environment != null ? environment : CourtEnvironmentEnum.OUTDOOR;
        WeatherEnum resolvedWeather = weather != null ? weather : WeatherEnum.CLEAR;
        if (resolvedEnvironment == CourtEnvironmentEnum.INDOOR) {
            return valueOf("INDOOR_" + resolvedSurface.name());
        }
        return valueOf(resolvedSurface.name() + "_" + resolveSlot(startTime).name() + "_" + resolvedWeather.name());
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
