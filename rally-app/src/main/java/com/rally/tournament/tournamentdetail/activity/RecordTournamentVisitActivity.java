package com.rally.tournament.tournamentdetail.activity;

import com.rally.domain.tournament.model.TournamentDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 record-tournament-visit：在赛事详情读取链路中记录已报名用户的访问时间。
 *
 * <p>既有详情聚合在定位本人报名后立即覆盖 {@code lastVisitTime}，再继续组装个人详情。
 * 本活动保留该调用边界，不增加外层事务或异常吞吐，以确保后续聚合失败时已完成的
 * 访问写入不会被回滚。</p>
 */
@Component
@RequiredArgsConstructor
public class RecordTournamentVisitActivity {

    private final AssembleTournamentDetailActivity assembleTournamentDetailActivity;

    /**
     * 匿名或未报名访问由详情聚合直接跳过；已报名访问先记录时间，再继续详情聚合。
     */
    public TournamentDetailDTO execute(String tournamentId, String userId) {
        return assembleTournamentDetailActivity.execute(tournamentId, userId);
    }
}
