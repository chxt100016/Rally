package com.rally.domain.tournament.model;

import com.rally.domain.user.enums.GenderEnum;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户展示信息投影：昵称/头像/性别/NTRP，nickname/avatarUrl/gender/ntrpScore 由 app 层批量回填，领域层不跨域查询
 */
@Data
public class MatchOpponentDTO {
    private String userId;
    private String nickname;
    private String avatarUrl;
    private GenderEnum gender;
    private BigDecimal ntrpScore;
}
