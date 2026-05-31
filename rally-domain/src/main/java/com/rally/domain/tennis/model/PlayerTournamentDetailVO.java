package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 球员弹窗 - 球员基本信息
 */
@Data
public class PlayerTournamentDetailVO {

    private String id;
    /** 头像URL，暂时为 null，后续扩展 */
    private String avatarUrl;
    private String name;
    private CountryVO country;
    private Integer rank;
    private Integer points;
    private Integer age;
    /** 种子号，无种子为 null */
    private Integer seed;
}
