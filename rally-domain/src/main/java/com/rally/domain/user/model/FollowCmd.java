package com.rally.domain.user.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 关注/取消关注入参
 */
@Data
public class FollowCmd {
    /** 目标用户 user_id */
    @NotBlank(message = "目标用户不能为空")
    private String targetUserId;
}
