package com.rally.db.tour.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("tour_tournament")
public class TourTournamentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tournamentId;
    private Integer year;
    private String name;
    private String tour;
    private String category;
    private String surface;
    private String city;
    private String country;
    private Integer prizeMoney;
    private String prizeMoneyText;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imagePath;
    private String backgroundPath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
