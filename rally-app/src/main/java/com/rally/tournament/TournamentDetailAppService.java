package com.rally.tournament;

import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.tournament.tournamentdetail.activity.RecordTournamentVisitActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 赛事详情应用入口，保留既有公开接口并委托详情聚合活动。
 */
@Service
@RequiredArgsConstructor
public class TournamentDetailAppService {

    private final RecordTournamentVisitActivity recordTournamentVisitActivity;

    /**
     * 赛事落地页详情，userId 从 UserContext 取，可匿名（未登录只返回公开区块）。
     */
    public TournamentDetailDTO detail(String tournamentId) {
        return recordTournamentVisitActivity.execute(tournamentId, UserContext.getIfPresent());
    }
}
