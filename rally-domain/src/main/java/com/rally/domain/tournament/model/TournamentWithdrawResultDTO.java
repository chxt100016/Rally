package com.rally.domain.tournament.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退出赛事结果返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentWithdrawResultDTO {
    /** 是否触发退款（正赛阶段退出） */
    private boolean refundTriggered;
}
