package com.rally.domain.meetup.scorerecord;

/** 一盘比分的原始双方阵容；领域不校验人数结构、重复或参与资格。 */
public record ScoreLineup(
        ScorePlayerSnapshot sideAPlayer1,
        ScorePlayerSnapshot sideAPlayer2,
        ScorePlayerSnapshot sideBPlayer1,
        ScorePlayerSnapshot sideBPlayer2) {
}
