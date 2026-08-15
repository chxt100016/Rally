package com.rally.tour.parser;

import lombok.Getter;

@Getter
public enum CollectType {

    ATP_DRAW(Phase.DRAW, "https://api.tennistv.com/tennis/v1/tournaments/%s/%d/draws"),
    ATP_APP_DRAW(Phase.DRAW, "https://app.atptour.com/api/v2/gateway/draws/ms"),
    WTA_DRAW(Phase.DRAW, "https://wta-webapi-prod-apimanagement.azure-api.net/atpjoint-api/v1/TournamentDraws/draws"),
    ATP_OOP(Phase.OOP, "https://api.tennistv.com/tennis/v1/oop"),
    WTA_OOP(Phase.OOP, "https://api.wtatennis.com/tour/tournaments/%s/%d/matches"),
    ATP_LIVE(Phase.LIVE, "https://api.tennistv.com/tennis/v1/matches"),
    WTA_LIVE(Phase.LIVE, "https://api.wtatennis.com/tour/tournaments/%s/%d/matches"),
    WTA_SCHEDULE(Phase.OOP, "https://wta-webapi-prod-apimanagement.azure-api.net/atpjoint-api/v1/Scores/Schedule"),
    ATP_SCHEDULE(Phase.OOP, "https://app.atptour.com/api/v2/gateway/scores/schedule"),
    ATP_SCHEDULE_FOR_WTA(Phase.OOP, "https://app.atptour.com/api/v2/gateway/scores/schedule"),
    ATP_APP_LIVE(Phase.LIVE, "https://app.atptour.com/api/v2/gateway/livematches"),
    ATP_APP_COMPLETED(Phase.DRAW, "https://app.atptour.com/api/v2/gateway/results/completed"),
    ;

    @Getter
    public enum Phase {
        DRAW(60),
        OOP(60),
        LIVE(5);

        private final int intervalMinutes;

        Phase(int intervalMinutes) {
            if (intervalMinutes <= 0) {
                throw new IllegalArgumentException("采集间隔必须大于 0");
            }
            this.intervalMinutes = intervalMinutes;
        }

        public boolean shouldRun(long epochMinute) {
            return Math.floorMod(epochMinute, intervalMinutes) == 0;
        }

        static {
            int baseInterval = LIVE.intervalMinutes;
            for (Phase phase : values()) {
                if (phase.intervalMinutes % baseInterval != 0) {
                    throw new IllegalStateException("Phase 间隔必须是 LIVE 间隔的整数倍: " + phase);
                }
            }
        }
    }

    private final Phase phase;
    private final String apiUrl;

    CollectType(Phase phase, String apiUrl) {
        this.phase = phase;
        this.apiUrl = apiUrl;
    }

}
