package com.rally.db.court.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 球场信息表 PO
 */
@Data
@TableName("rally_court")
public class CourtPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizId;
    private String name;
    private String address;
    private Double lng;
    private Double lat;
    private String cityCode;
    private String districtCode;
    private Integer total;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
