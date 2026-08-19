package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * myCurrentMatch 内的参与者投影
 */
@Data
public class MatchParticipantDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
    /** 仅在当前用户查看自己的比赛时返回对手手机号。 */
    private String phone;
    private Integer entryNo;
    /** 对手最近一次打开本赛事详情的时间；本人及同队成员不返回。 */
    private LocalDateTime lastVisitTime;
    private ConfirmStatusEnum confirmStatus;
    private ConfirmStatusEnum resultConfirmStatus;
}
