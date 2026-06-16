package com.rally.domain.user.model;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 关注/被关注列表查询入参
 */
@Data
public class FollowListCmd {
    /** 目标用户 user_id，为空则查询当前登录用户 */
    private String userId;
    /** 上一页最后一条的 bizId，首页不传 */
    private String lastId;
    /** 每页数量，默认20 */
    @Min(value = 1, message = "每页数量最小为1")
    private Integer size = 20;
}
