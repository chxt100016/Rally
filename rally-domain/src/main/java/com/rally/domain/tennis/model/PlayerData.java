package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 球员数据模型（domain 层）
 */
@Data
public class PlayerData {

    private String playerId;
    private String firstName;
    private String lastName;
    private String nationality;
}
