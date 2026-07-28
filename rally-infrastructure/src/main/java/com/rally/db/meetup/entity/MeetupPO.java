package com.rally.db.meetup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private String meetupType;
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
    /** 球场选择模式：TEXT/MAP/FREE */
    private String courtSelectMode;
    /** 球场库ID */
    private String courtId;

    private String levelMode;
    private BigDecimal levelMin;
    private BigDecimal levelMax;
    private String genderLimit;
    private String joinMode;
    /** 费用数据 JSON */
    @TableField("cost_data")
    private String costData;
    private String status;
    /** 场地索引，前端透传存储 */
    private String courtIndex;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 待处理原因（SQL 计算列，非表字段） */
    @TableField(exist = false)
    private String pendingReason;
    /** 距离（米），距离排序 SQL 计算列，非表字段 */
    @TableField(exist = false)
    private Double distanceMeters;
}
