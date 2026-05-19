package com.rally.tennis.parser;

import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;

import java.util.List;

public abstract class MatchParser<P, S> {

    /** 调用 Client 获取数据并拆分出多个签表切片，同时填充 tournamentId / year / discipline */
    public abstract List<DrawResult<S>> fetchDraws(P params);

    /** 从切片提取所有比赛 */
    public abstract List<Match> getMatches(DrawResult<S> draw, String tournamentId, Long drawId);

    /** 从切片提取所有参赛球员 */
    public abstract List<Player> getPlayers(DrawResult<S> draw);

    /** 从切片提取参赛资格/种子信息 */
    public abstract List<TournamentEntry> getEntries(DrawResult<S> draw, Long drawId);

    /** 标识当前 Parser 对应的采集类型，用于 Manager 构建路由 Map */
    public abstract CollectType collectType();

    /** Live 类 Parser 返回 true，Manager 会调用 updateMatches 而非 saveMatches */
    public boolean isLive() {
        return false;
    }
}
