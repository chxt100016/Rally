package com.rally.tennis.parser;

import com.rally.tennis.model.Match;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TournamentEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * R: Client 接口的原始返回类型，由 request 获取，在 ms/fetchMd/fetchLs/fetchLd 间共享
 * S: 单个签表切片类型，用于 getMatches/getPlayers/getEntries
 */
public abstract class MatchParser<R, S> {

    /** 模板方法：依次调用 ms/fetchMd/fetchLs/fetchLd 收集各类签表，子类按需覆盖对应方法 */
    public final List<DrawResult<S>> fetch(DrawParams params) {
        R data = request(params);
        List<DrawResult<S>> results = new ArrayList<>();
        results.addAll(ms(data, params));
        results.addAll(md(data, params));
        results.addAll(ls(data, params));
        results.addAll(ld(data, params));
        return results;
    }

    /** 调用 Client 获取原始数据，供各 draw 方法共享 */
    protected R request(DrawParams params) { return null; }

    protected List<DrawResult<S>> ms(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> md(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> ls(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> ld(R data, DrawParams params) { return List.of(); }

    /** 从切片提取所有比赛 */
    public abstract List<Match> getMatches(DrawResult<S> draw, String tournamentId, Long drawId);

    /** 从切片提取所有参赛球员 */
    public abstract List<Player> getPlayers(DrawResult<S> draw);

    /** 从切片提取参赛资格/种子信息 */
    public abstract List<TournamentEntry> getEntries(DrawResult<S> draw, Long drawId);

    /** 标识当前 Parser 对应的采集类型，用于 Manager 构建路由 Map */
    public abstract CollectType collectType();
}
