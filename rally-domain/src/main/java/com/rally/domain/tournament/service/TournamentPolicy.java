package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 赛事断言服务（收纳创建/编辑等场景的校验逻辑）
 */
@Service
public class TournamentPolicy {

    private static final Set<Integer> VALID_TOTAL_SLOTS = Set.of(16, 32, 64);

    /**
     * 创建/编辑参数校验：签位数枚举合法、offlineFromRound < totalSlots、entryFee ≥ 0、时间点先后
     */
    public void assertParam(TournamentCreateCmd cmd) {
        if (!VALID_TOTAL_SLOTS.contains(cmd.getTotalSlots())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "正赛签位数只能是16/32/64");
        }
        if (cmd.getOfflineFromRound() >= cmd.getTotalSlots()) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "转线下轮次必须小于正赛签位数");
        }
        if (cmd.getEntryFee() < 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "报名费不能为负");
        }
        if (!cmd.getRegistrationStartTime().isBefore(cmd.getQualifierStartTime())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_TIME_ILLEGAL, "报名开始时间必须早于资格赛开始时间");
        }
        if (cmd.getRegistrationEndTime() != null && cmd.getRegistrationEndTime().isBefore(cmd.getRegistrationStartTime())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_TIME_ILLEGAL, "报名截止时间不能早于报名开始时间");
        }
        if (cmd.getQualifierEndTime() != null && cmd.getQualifierEndTime().isBefore(cmd.getQualifierStartTime())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_TIME_ILLEGAL, "资格赛截止时间不能早于资格赛开始时间");
        }
    }

    /**
     * 报名校验：赛事 ACTIVE、在报名开放窗口内、性别限制符合
     */
    public void assertCanJoin(Tournament tournament, UserProfile userProfile) {
        Assert.isTrue(tournament.getData().getStatus() == TournamentStatusEnum.ACTIVE, BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
        LocalDateTime now = LocalDateTime.now();
        boolean inWindow = !now.isBefore(tournament.getData().getRegistrationStartTime())
                && (tournament.getData().getRegistrationEndTime() == null || !now.isAfter(tournament.getData().getRegistrationEndTime()));
        Assert.isTrue(inWindow, BizErrorCode.TOURNAMENT_REGISTRATION_CLOSED);
        assertGenderMatch(tournament, userProfile);
    }

    private void assertGenderMatch(Tournament tournament, UserProfile userProfile) {
        TournamentGenderLimitEnum genderLimit = tournament.getData().getGenderLimit();
        if (genderLimit == TournamentGenderLimitEnum.ALL) {
            return;
        }
        GenderEnum userGender = userProfile.getGender();
        if (userGender == null) {
            return;
        }
        if (genderLimit == TournamentGenderLimitEnum.MALE) {
            Assert.isTrue(userGender == GenderEnum.MALE, BizErrorCode.GENDER_NOT_MATCH);
        } else if (genderLimit == TournamentGenderLimitEnum.FEMALE) {
            Assert.isTrue(userGender == GenderEnum.FEMALE, BizErrorCode.GENDER_NOT_MATCH);
        }
    }
}
