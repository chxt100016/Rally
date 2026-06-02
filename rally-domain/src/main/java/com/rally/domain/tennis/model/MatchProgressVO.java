package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 球员弹窗 - 晋级路线 / 前方对手 / 出局信息 条目
 */
@Data
public class MatchProgressVO {

    private String round;
    private String roundLabel;
    private String opponentId;
    private String opponentName;
    /** 对手国籍 */
    private CountryVO opponentCountry;
    /** 对手种子号，无种子为 null */
    private Integer opponentSeed;
    /** 比分，未完成时为"待定" */
    private String score;
    /** WIN / LOSS / PENDING */
    private String result;
}
