package com.rally.domain.score.model;

import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.recap.gateway.ReviewRepository;
import com.rally.domain.log.gateway.ProfileChangeLogRepository;
import com.rally.domain.user.gateway.TennisProfileRepository;
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
    private ReviewRepository reviewRepository;


    /** 只读 Gateway：约球数据（参与者、finished 判定） */
    private MeetupRepository meetupRepository;

    /** 写 Gateway：球员档案（三维分/核查期） */
    private TennisProfileRepository profileRepository;

    /** 写 Gateway：变更日志 */
    private ProfileChangeLogRepository changeLogRepository;
}
