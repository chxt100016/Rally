package com.rally.domain.meetup.scorerecord;

/** rally_meetup_score.set_format 的领域枚举。 */
public enum ScoreSetFormat {
    GAME,
    TIEBREAK;

    public String storageValue() {
        return name();
    }
}
