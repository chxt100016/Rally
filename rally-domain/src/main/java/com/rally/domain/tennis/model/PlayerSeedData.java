package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 球员种子数据（domain 层）
 */
@Data
public class PlayerSeedData {

    private String tournamentId;
    private String playerId;
    private Integer seed;
}
