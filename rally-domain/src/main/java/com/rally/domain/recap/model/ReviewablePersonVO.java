package com.rally.domain.recap.model;

import com.rally.domain.recap.enums.AttendanceEnum;
import com.rally.domain.recap.enums.NtrpVoteEnum;
import lombok.Data;

import java.util.List;

/**
 * 单个可评价人 VO
 */
@Data
public class ReviewablePersonVO {
    /** 被评价人 user_id */
    private String toUserId;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatarUrl;
    /** 我是否已评过该人 */
    private Boolean reviewed;
    /** 我已提交的 NTRP 投票（回填表单） */
    private NtrpVoteEnum ntrpVote;
    /** 我已提交的出勤标签（回填表单） */
    private AttendanceEnum attendance;
    /** 我已提交的个性化标签（回填表单） */
    private List<String> tags;
    /** 标签推荐池：系统默认 ∪ 历史随机挑3 */
    private List<String> tagSuggestions;
}
