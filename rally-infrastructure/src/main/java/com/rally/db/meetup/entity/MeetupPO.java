package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 约球主表 PO
 */
@Data
@TableName("rally_meetup")
public class MeetupPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String creatorId;
    private String title;
    private String matchType;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String cityCode;
    private String cityName;
    private String districtName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal duration;
    private String courtName;
    private String courtAddress;
    /** 经度（从 POINT 解构） */
    private Double courtLng;
    /** 纬度（从 POINT 解构） */
    private Double courtLat;

    private String levelMode;
    private String levelValue;
    private String genderLimit;
    private String joinMode;
    /** 费用明细 JSON */
    private String costItems;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
