package com.rally.db.tour.entity;

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
@TableName("tour_tournament_entry")
public class TourTournamentEntryPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String playerId;
    private Long drawId;
    private Short seed;
    private String entryType;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
