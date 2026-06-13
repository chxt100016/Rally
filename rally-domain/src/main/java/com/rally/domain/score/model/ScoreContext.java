package com.rally.domain.score.model;

import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.recap.gateway.ReviewGateway;
import com.rally.domain.log.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
import lombok.Data;

import java.util.List;

/**
 * 单次评分计算上下文
 */
@Data
public class ScoreContext {

    /** 约球 biz_id */
    private String meetupId;

    /** 参与者 user_id 列表（发布者 + 已批准报名者） */
    private List<String> participants;

    /** 评分状态版本（用于幂等控制） */
    private Integer scoreVersion;

    /** 只读 Gateway：评价数据 */
    private ReviewGateway reviewGateway;


    /** 只读 Gateway：约球数据（参与者、finished 判定） */
    private MeetupGateway meetupGateway;

    /** 写 Gateway：球员档案（三维分/核查期） */
    private TennisProfileGateway profileGateway;

    /** 写 Gateway：变更日志 */
    private ProfileChangeLogGateway changeLogGateway;
}
