package com.rally.db.tennis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tennis_draw")
public class TennisDrawPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tournamentId;
    private Integer year;
    private String drawType;
    private Integer size;
    private Integer totalRounds;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
