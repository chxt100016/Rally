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
    WTA_SCHEDULE(Phase.OOP),
    ATP_SCHEDULE(Phase.OOP),
    ATP_SCHEDULE_FOR_WTA(Phase.OOP),
    ATP_APP_LIVE(Phase.LIVE),
    ATP_APP_COMPLETED(Phase.DRAW),
    ;

    public enum Phase { DRAW, OOP, LIVE }

    private final Phase phase;

    CollectType(Phase phase) {
        this.phase = phase;
    }

}
