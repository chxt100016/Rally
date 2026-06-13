package com.rally.db.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比分记录 PO（按盘，乐观锁）
 */
@Data
@TableName("rally_meetup_score")
public class ScoreRecordPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String rallyMeetupId;
    /** 第几盘，从 1 开始 */
    private Integer setNumber;
    /** 赛制：games_4 / games_6 / tiebreak */
    private String setFormat;
    /** A 侧选手1 user_id */
    private String sideAPlayer1;
    /** A 侧选手1昵称（冗余存储） */
    private String sideAPlayer1Nickname;
    /** A 侧选手1头像URL（冗余存储） */
    private String sideAPlayer1Avatar;
    /** A 侧选手2 user_id，单打为 NULL */
    private String sideAPlayer2;
    /** A 侧选手2昵称（冗余存储） */
    private String sideAPlayer2Nickname;
    /** A 侧选手2头像URL（冗余存储） */
    private String sideAPlayer2Avatar;
    /** B 侧选手1 user_id */
    private String sideBPlayer1;
    /** B 侧选手1昵称（冗余存储） */
    private String sideBPlayer1Nickname;
    /** B 侧选手1头像URL（冗余存储） */
    private String sideBPlayer1Avatar;
    /** B 侧选手2 user_id，单打为 NULL */
    private String sideBPlayer2;
    /** B 侧选手2昵称（冗余存储） */
    private String sideBPlayer2Nickname;
    /** B 侧选手2头像URL（冗余存储） */
    private String sideBPlayer2Avatar;
    /** A 侧本盘比分 */
    private Integer sideAScore;
    /** B 侧本盘比分 */
    private Integer sideBScore;
    /** 记录人 user_id */
    private String recordedBy;
    /** 乐观锁版本号 */
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
