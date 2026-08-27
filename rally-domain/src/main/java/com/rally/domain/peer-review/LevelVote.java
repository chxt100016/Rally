package com.rally.domain.meetup.peerreview;

import java.util.Locale;

/** 水平三元投票值及其持久化编码。 */
public enum LevelVote {
    HIGHER("higher"),
    SAME("same"),
    LOWER("lower");

    private final String storageValue;

    LevelVote(String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return storageValue;
    }

    static LevelVote parse(String value) {
        if (value != null) {
            String normalized = value.strip().toUpperCase(Locale.ROOT);
            for (LevelVote vote : values()) {
                if (vote.name().equals(normalized)
                        || vote.storageValue.equals(normalized.toLowerCase(Locale.ROOT))) {
                    return vote;
                }
            }
        }
        throw new PeerReviewDomainException(
                PeerReviewSet.PEER_REVIEW_VALUE_INVALID,
                "水平评价只接受 HIGHER、SAME 或 LOWER");
    }
}
