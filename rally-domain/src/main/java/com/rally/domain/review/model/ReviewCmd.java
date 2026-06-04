package com.rally.domain.review.model;

import com.rally.domain.review.enums.AttendanceEnum;
import com.rally.domain.review.enums.NtrpVoteEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提交评价入参（本人对某人，一次覆盖全部维度）
 */
@Data
public class ReviewCmd {
    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String rallyMeetupId;

    /** 被评价人 user_id */
    @NotBlank(message = "被评价人不能为空")
    private String toUserId;

    /** NTRP 三元投票 */
    @NotNull(message = "请选择水平评价")
    private NtrpVoteEnum ntrpVote;

    /** 出勤标签 */
    @NotNull(message = "请选择出勤状态")
    private AttendanceEnum attendance;

    /** 个性化标签名列表（0~N 个，含手动输入） */
    @Size(max = 10, message = "标签数量不超过10个")
    private List<String> tags;
}
