package com.rally.domain.score.model;

import com.rally.domain.user.enums.ChangeReasonEnum;
import lombok.Data;

/**
 * 单点信誉分扣减入参（约球域 02 调用）
 */
@Data
public class ReputationPenaltyCmd {

    /** 用户 ID */
    private String userId;

    /** 扣分原因（退出<6h、发布者关闭阶梯等） */
    private ChangeReasonEnum reason;

    /** 关联约球 biz_id */
    private String refMeetupId;
}
