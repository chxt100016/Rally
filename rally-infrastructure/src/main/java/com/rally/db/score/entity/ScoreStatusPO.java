package com.rally.db.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量评分幂等状态表 PO
 */
@Data
@TableName("rally_meetup_score_status")
public class ScoreStatusPO {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务唯一 ID（雪花算法字符串） */
    private String bizId;

    /** 关联 rally_meetup.biz_id */
    private String meetupId;

    /** 重算版本：评价/比分变更时 +1 */
    private Integer scoreVersion;

    /** 已处理到的版本号，初始 -1（从未处理） */
    private Integer processedVersion;

    /** 最近一次处理完成时间，NULL=从未处理 */
    private LocalDateTime processedAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
