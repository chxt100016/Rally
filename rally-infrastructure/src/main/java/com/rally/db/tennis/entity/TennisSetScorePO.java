package com.rally.db.tennis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tennis_set_score")
public class TennisSetScorePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String matchId;
    @TableField("tournament_id")
    private String tournamentId;
    private Integer year;
    @TableField("set_number")
    private Integer setNumber;
    @TableField("p1_games")
    private Integer p1Games;
    @TableField("p2_games")
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
