package com.rally.tennis.parser;

import lombok.Getter;

@Getter
public enum CollectType {
    ATP_DRAW(Phase.DRAW),
    ATP_APP_DRAW(Phase.DRAW),
    WTA_DRAW(Phase.DRAW),
    ATP_OOP(Phase.OOP),
    WTA_OOP(Phase.OOP),
    ATP_LIVE(Phase.LIVE),
    WTA_LIVE(Phase.LIVE),
    WTA_SCHEDULE(Phase.OOP);

    public enum Phase { DRAW, OOP, LIVE }

    private final Phase phase;

    CollectType(Phase phase) {
        this.phase = phase;
    }

}
