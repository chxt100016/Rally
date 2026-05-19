package com.rally.tennis.parser;

import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * R: Client 接口的原始返回类型，由 fetchData 获取，在 fetchMs/fetchMd/fetchLs/fetchLd 间共享
 * S: 单个签表切片类型，用于 getMatches/getPlayers/getEntries
 */
public abstract class MatchParser<R, S> {

    /** 模板方法：依次调用 fetchMs/fetchMd/fetchLs/fetchLd 收集各类签表，子类按需覆盖对应方法 */
    public final List<DrawResult<S>> fetchDraws(DrawParams params) {
        R data = fetchData(params);
        List<DrawResult<S>> results = new ArrayList<>();
        results.addAll(fetchMs(data, params));
        results.addAll(fetchMd(data, params));
        results.addAll(fetchLs(data, params));
        results.addAll(fetchLd(data, params));
        return results;
    }

    /** 调用 Client 获取原始数据，供各 draw 方法共享 */
    protected R fetchData(DrawParams params) { return null; }

    protected List<DrawResult<S>> fetchMs(R data, DrawParams params) { return List.of(); }
    protected List<DrawResult<S>> fetchMd(R data, DrawParams params) { return List.of(); }
    protected List<DrawResult<S>> fetchLs(R data, DrawParams params) { return List.of(); }
    protected List<DrawResult<S>> fetchLd(R data, DrawParams params) { return List.of(); }

    /** 从切片提取所有比赛 */
    public abstract List<Match> getMatches(DrawResult<S> draw, String tournamentId, Long drawId);

    /** 从切片提取所有参赛球员 */
    public abstract List<Player> getPlayers(DrawResult<S> draw);

    /** 从切片提取参赛资格/种子信息 */
    public abstract List<TournamentEntry> getEntries(DrawResult<S> draw, Long drawId);

    /** 标识当前 Parser 对应的采集类型，用于 Manager 构建路由 Map */
    public abstract CollectType collectType();
}
