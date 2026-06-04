package com.rally.db.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ELO 聚合表 PO
 */
@Data
@TableName("player_elo")
public class PlayerEloPO {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一 ID（雪花算法字符串） */
    private String bizId;

    /** 关联 users.user_id */
    private String userId;

    /** ELO 分，初始 1500 */
    private Float eloScore;

    /** 已计入 ELO 的对局盘数 */
    private Integer matchCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
