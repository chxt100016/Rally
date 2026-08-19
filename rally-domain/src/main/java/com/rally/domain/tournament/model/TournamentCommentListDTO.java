package com.rally.domain.tournament.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 赛事评论查询结果，comments 按时间倒序排列。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentCommentListDTO {

    private List<TournamentCommentDTO> comments;
}
