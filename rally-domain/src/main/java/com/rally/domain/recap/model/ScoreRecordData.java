package com.rally.domain.recap.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
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
    /** 比赛类型 */
    private MatchTypeEnum matchType;
    /** 比赛日期（冗余自 rally_meetup.start_time） */
    private LocalDateTime meetupDate;
    /** 比赛场地名称（冗余自 rally_meetup.court_name） */
    private String venueName;
    /** A 侧选手1 user_id */
    private String sideAPlayer1;
    /** A 侧选手1昵称（冗余存储） */
    private String sideAPlayer1Nickname;
    /** A 侧选手1头像URL（冗余存储） */
    private String sideAPlayer1Avatar;
    /** A 侧选手2 user_id（单打为 null） */
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
    /** B 侧选手2 user_id（单打为 null） */
    private String sideBPlayer2;
    /** B 侧选手2昵称（冗余存储） */
    private String sideBPlayer2Nickname;
    /** B 侧选手2头像URL（冗余存储） */
    private String sideBPlayer2Avatar;
    /** A 侧本盘比分 */
    private Integer sideAScore;
    /** B 侧本盘比分 */
    private Integer sideBScore;
    /** A 侧抢七比分（本盘 6:6 时记录） */
    private Integer sideATiebreakScore;
    /** B 侧抢七比分（本盘 6:6 时记录） */
    private Integer sideBTiebreakScore;
    /** 获胜边: A / B */
    private String winSide;
    /** 记录人 user_id */
    private String recordedBy;
    /** 乐观锁版本号 */
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
