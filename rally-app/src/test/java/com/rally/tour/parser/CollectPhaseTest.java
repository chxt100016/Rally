package com.rally.tour.parser;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectPhaseTest {

    @Test
    public void allPhasesRunOnHourlyBoundary() {
        long hourlyBoundary = 60L * 1_000_000;

        assertTrue(CollectType.Phase.LIVE.shouldRun(hourlyBoundary));
        assertTrue(CollectType.Phase.OOP.shouldRun(hourlyBoundary));
        assertTrue(CollectType.Phase.DRAW.shouldRun(hourlyBoundary));
    }

    @Test
    public void onlyLiveRunsOnRegularFiveMinuteBoundary() {
        long fiveMinuteBoundary = 60L * 1_000_000 + 5;

        assertTrue(CollectType.Phase.LIVE.shouldRun(fiveMinuteBoundary));
        assertFalse(CollectType.Phase.OOP.shouldRun(fiveMinuteBoundary));
        assertFalse(CollectType.Phase.DRAW.shouldRun(fiveMinuteBoundary));
    }
}
