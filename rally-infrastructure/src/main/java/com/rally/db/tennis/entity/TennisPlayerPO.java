package com.rally.db.tennis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tennis_player")
public class TennisPlayerPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String playerId;
    /** 所属巡回赛：ATP / WTA */
    private String tour;
    private String firstName;
    private String lastName;
    private String nationality;
    private LocalDate birthDate;
    private String gender;
    @TableField("`rank`")
    private Integer rank;
    private Integer points;
    private String hand;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
