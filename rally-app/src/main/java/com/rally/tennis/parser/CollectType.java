package com.rally.tennis.parser;

public enum CollectType {
    ATP_DRAW(Phase.DRAW),
    WTA_DRAW(Phase.DRAW),
    ATP_OOP(Phase.OOP),
    WTA_OOP(Phase.OOP),
    ATP_LIVE(Phase.LIVE),
    WTA_LIVE(Phase.LIVE);

    public enum Phase { DRAW, OOP, LIVE }

    private final Phase phase;

    CollectType(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }
}
