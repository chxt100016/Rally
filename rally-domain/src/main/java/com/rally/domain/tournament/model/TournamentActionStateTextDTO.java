package com.rally.domain.tournament.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛事当前状态卡片文案。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentActionStateTextDTO {
    private String title;
    private String subtitle;
}
