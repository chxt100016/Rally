package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentJoinRestrictionEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 赛事断言服务（收纳创建/编辑等场景的校验逻辑）
 */
@Service
public class TournamentPolicy {

    /**
     * 创建/编辑参数校验：签位数为 2 到 64 的 2 次方；可选的 offlineFromRound 对应签位数 < totalSlots；entryFee ≥ 0；时间点先后合法。
     */
    public void assertParam(TournamentCreateCmd cmd) {
        if (!isSupportedPowerOfTwo(cmd.getTotalSlots())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "正赛签位数只能是2到64的2次方");
        }
        if (cmd.getOfflineFromRound() != null && cmd.getOfflineFromRound().getSlotCount() >= cmd.getTotalSlots()) {
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

    private boolean isSupportedPowerOfTwo(Integer totalSlots) {
        return totalSlots != null
                && totalSlots >= 2
                && totalSlots <= 64
                && (totalSlots & (totalSlots - 1)) == 0;
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
        assertPhoneBound(userProfile);
        assertGenderMatch(tournament, userProfile);
        assertNtrpLevelMatch(tournament, userProfile);
    }

    private void assertNtrpLevelMatch(Tournament tournament, UserProfile userProfile) {
        boolean levelMatch = isNtrpLevelMatch(tournament.getData().getNtrpLevel(), userProfile);
        Assert.isTrue(levelMatch, BizErrorCode.TOURNAMENT_NTRP_LEVEL_NOT_MATCH);
    }

    /**
     * 判断用户 NTRP 等级是否符合赛事要求，与报名接口使用同一数值相等规则。
     */
    public boolean isNtrpLevelMatch(String requiredNtrpLevel, UserProfile userProfile) {
        BigDecimal tournamentNtrpLevel = new BigDecimal(requiredNtrpLevel);
        BigDecimal userNtrpLevel = userProfile.getProfile() == null ? null : userProfile.getProfile().getNtrpScore();
        return userNtrpLevel != null && userNtrpLevel.compareTo(tournamentNtrpLevel) == 0;
    }

    /**
     * 收集赛事未报名场景下的用户准入限制。
     * userProfile 为空表示用户未登录；网球档案未完善时由完善状态承接提示，不重复返回等级不符。
     */
    public List<TournamentJoinRestrictionEnum> collectJoinRestrictions(String requiredNtrpLevel,
                                                                        TournamentGenderLimitEnum genderLimit,
                                                                        UserProfile userProfile) {
        List<TournamentJoinRestrictionEnum> restrictions = new ArrayList<>();
        if (userProfile == null) {
            restrictions.add(TournamentJoinRestrictionEnum.NOT_LOGGED_IN);
            return restrictions;
        }

        boolean basicDefault = userProfile.getUser().isBasicDefault();
        boolean profileIncomplete = !userProfile.hasProfile();
        if (basicDefault && profileIncomplete) {
            restrictions.add(TournamentJoinRestrictionEnum.REGISTRATION_INCOMPLETE);
        } else if (basicDefault) {
            restrictions.add(TournamentJoinRestrictionEnum.PROFILE_INCOMPLETE);
        } else if (profileIncomplete) {
            restrictions.add(TournamentJoinRestrictionEnum.ONBOARDING_INCOMPLETE);
        }
        if (!isGenderMatch(genderLimit, userProfile)) {
            restrictions.add(TournamentJoinRestrictionEnum.GENDER_NOT_MATCH);
        }
        if (!profileIncomplete && !isNtrpLevelMatch(requiredNtrpLevel, userProfile)) {
            restrictions.add(TournamentJoinRestrictionEnum.LEVEL_NOT_MATCH);
        }
        addPhoneRestriction(restrictions, userProfile);
        return restrictions;
    }

    public List<TournamentJoinRestrictionEnum> collectPhoneRestrictions(UserProfile userProfile) {
        List<TournamentJoinRestrictionEnum> restrictions = new ArrayList<>();
        if (userProfile == null) {
            restrictions.add(TournamentJoinRestrictionEnum.NOT_LOGGED_IN);
            return restrictions;
        }
        addPhoneRestriction(restrictions, userProfile);
        return restrictions;
    }

    public void assertPhoneBound(UserProfile userProfile) {
        Assert.isTrue(userProfile != null && userProfile.getUser() != null && userProfile.getUser().hasPhone(), BizErrorCode.USER_PHONE_REQUIRED);
    }

    public void assertCanUnfreeze(Tournament tournament) {
        Assert.isTrue(tournament.getData().getStatus() == TournamentStatusEnum.ACTIVE, BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
        LocalDateTime endTime = tournament.getData().getEndTime();
        Assert.isTrue(endTime == null || !LocalDateTime.now().isAfter(endTime), BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
    }

    private void addPhoneRestriction(List<TournamentJoinRestrictionEnum> restrictions, UserProfile userProfile) {
        if (userProfile.getUser() == null || !userProfile.getUser().hasPhone()) {
            restrictions.add(TournamentJoinRestrictionEnum.PHONE_MISSING);
        }
    }

    private void assertGenderMatch(Tournament tournament, UserProfile userProfile) {
        boolean genderMatch = isGenderMatch(tournament.getData().getGenderLimit(), userProfile);
        Assert.isTrue(genderMatch, BizErrorCode.GENDER_NOT_MATCH);
    }

    private boolean isGenderMatch(TournamentGenderLimitEnum genderLimit, UserProfile userProfile) {
        if (genderLimit == null || genderLimit == TournamentGenderLimitEnum.ALL) {
            return true;
        }
        GenderEnum userGender = userProfile.getGender();
        if (userGender == null) {
            return true;
        }
        if (genderLimit == TournamentGenderLimitEnum.MALE) {
            return userGender == GenderEnum.MALE;
        }
        return userGender == GenderEnum.FEMALE;
    }
}
