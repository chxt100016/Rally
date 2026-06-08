package com.rally.domain.recap.model;

import com.rally.domain.recap.enums.SetFormatEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比分记录领域数据对象（一盘一行）
 */
@Data
public class ScoreRecordData {
    /** 业务唯一 ID */
    private String bizId;
    /** 关联约球 biz_id */
    private String rallyMeetupId;
    /** 第几盘，从 1 开始 */
    private Integer setNumber;
    /** 赛制 */
    private SetFormatEnum setFormat;
    /** A 侧选手1 user_id */
    private String sideAPlayer1;
    /** A 侧选手2 user_id（单打为 null） */
    private String sideAPlayer2;
    /** B 侧选手1 user_id */
    private String sideBPlayer1;
    /** B 侧选手2 user_id（单打为 null） */
    private String sideBPlayer2;
    /** A 侧本盘比分 */
    private Integer sideAScore;
    /** B 侧本盘比分 */
    private Integer sideBScore;
    /** 记录人 user_id */
    private String recordedBy;
    /** 乐观锁版本号 */
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
