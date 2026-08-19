package com.rally.domain.tournament.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户的赛事评论状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentCommentStateDTO {

    private Integer unreadCount;
}
