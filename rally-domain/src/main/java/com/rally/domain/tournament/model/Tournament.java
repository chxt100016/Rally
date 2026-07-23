package com.rally.domain.tournament.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Getter;

/**
 * 赛事聚合根（配置 + 席位进度 + 状态机 DRAFT → ACTIVE → ABANDONED）
 */
@Getter
public class Tournament {

    private final TournamentData data;

    public Tournament(TournamentData data) {
        this.data = data;
    }

    public String getTournamentId() {
        return this.data.getBizId();
    }

    /** 仅 DRAFT 可编辑 */
    public boolean canEdit() {
        return this.data.getStatus() == TournamentStatusEnum.DRAFT;
    }

    public void assertCanEdit() {
        Assert.isTrue(canEdit(), BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
    }

    /** 仅 DRAFT 可激活 */
    public void assertCanActivate() {
        Assert.isTrue(this.data.getStatus() == TournamentStatusEnum.DRAFT, BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
        if (this.data.getRegistrationStartTime() == null || this.data.getQualifierStartTime() == null) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_CONFIG_INCOMPLETE);
        }
        if (!this.data.getRegistrationStartTime().isBefore(this.data.getQualifierStartTime())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_TIME_ILLEGAL);
        }
    }

    /** ABANDONED 不可逆，任意非终止态可废弃 */
    public void assertCanAbandon() {
        Assert.isTrue(this.data.getStatus() != TournamentStatusEnum.ABANDONED, BizErrorCode.TOURNAMENT_STATUS_ILLEGAL);
    }

    public void activate() {
        assertCanActivate();
        this.data.setStatus(TournamentStatusEnum.ACTIVE);
    }

    public void abandon() {
        assertCanAbandon();
        this.data.setStatus(TournamentStatusEnum.ABANDONED);
    }

    public boolean isSlotsFull() {
        return this.data.getCurrentFilledSlots() >= this.data.getTotalSlots();
    }

    public void assertSlotsNotFull() {
        Assert.isTrue(!isSlotsFull(), BizErrorCode.TOURNAMENT_SLOTS_FULL);
    }
}
