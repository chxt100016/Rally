package com.rally.domain.court.enums;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourtBackgroundEnumTest {

    @Test
    public void shouldBeAnEnumAndResolveOutdoorBackgrounds() {
        assertTrue(CourtBackgroundEnum.class.isEnum());
        assertEquals("hard-day-clear", resolve(CourtSurfaceEnum.HARD, 6, 0, WeatherEnum.CLEAR));
        assertEquals("clay-dusk-rain", resolve(CourtSurfaceEnum.CLAY, 17, 0, WeatherEnum.RAIN));
        assertEquals("grass-night-clear", resolve(CourtSurfaceEnum.GRASS, 19, 0, WeatherEnum.CLEAR));
        assertEquals("hard-night-clear", resolve(CourtSurfaceEnum.HARD, 5, 59, WeatherEnum.CLEAR));
    }

    @Test
    public void shouldIgnoreTimeAndWeatherForIndoorCourt() {
        String key = CourtBackgroundEnum.resolveKey(
                CourtSurfaceEnum.CLAY,
                CourtEnvironmentEnum.INDOOR,
                LocalDateTime.of(2026, 8, 2, 22, 0),
                WeatherEnum.RAIN);

        assertEquals("indoor-clay", key);
    }

    @Test
    public void shouldUseDefaultsAndReturnNullWithoutStartTime() {
        assertEquals("hard-day-clear", CourtBackgroundEnum.resolveKey(
                null, null, LocalDateTime.of(2026, 8, 2, 12, 0), null));
        assertNull(CourtBackgroundEnum.resolveKey(
                CourtSurfaceEnum.HARD, CourtEnvironmentEnum.OUTDOOR, null, WeatherEnum.CLEAR));
    }

    private String resolve(CourtSurfaceEnum surface, int hour, int minute, WeatherEnum weather) {
        return CourtBackgroundEnum.resolveKey(
                surface,
                CourtEnvironmentEnum.OUTDOOR,
                LocalDateTime.of(2026, 8, 2, hour, minute),
                weather);
    }
}
