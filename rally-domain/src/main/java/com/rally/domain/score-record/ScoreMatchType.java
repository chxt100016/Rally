package com.rally.domain.meetup.scorerecord;

/** rally_meetup_score.match_type 的领域枚举。 */
public enum ScoreMatchType {
    SINGLE,
    DOUBLE,
    RALLY;

    public String storageValue() {
        return name();
    }
}
