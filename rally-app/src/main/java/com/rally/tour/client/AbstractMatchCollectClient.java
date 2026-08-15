package com.rally.tour.client;

import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TourEnums;
import com.rally.tour.model.TournamentEntry;
import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawParams;
import com.rally.tour.parser.DrawResult;

import java.util.ArrayList;
import java.util.List;

/**
 * R: Client 接口的原始返回类型，由 request 获取，在 ms/fetchMd/fetchLs/fetchLd 间共享
 * S: 单个签表切片类型，用于 getMatches/getPlayers/getEntries
 */
public abstract class AbstractMatchCollectClient<R, S> implements MatchCollectClient {

    /**
     * 模板方法：对应 client 完成请求、切分和标准模型转换，调用方不再接触上游响应类型。
     */
    @Override
    public final List<MatchCollectResult> collect(DrawParams params) {
        return fetch(params).stream().map(draw -> toCollectResult(draw, params)).toList();
    }

    private List<DrawResult<S>> fetch(DrawParams params) {
        R data = request(params);
        List<DrawResult<S>> results = new ArrayList<>();
        switch (TourEnums.valueOf(params.getTour())) {
            case ATP -> {
                results.addAll(ms(data, params));
                results.addAll(md(data, params));
            }
            case WTA -> {
                results.addAll(ls(data, params));
                results.addAll(ld(data, params));
            }
        }
        return results;
    }

    private MatchCollectResult toCollectResult(DrawResult<S> draw, DrawParams params) {
        List<Match> matches = getMatches(draw, draw.getTournamentId());
        matches.forEach(match -> match.setDrawType(draw.getDrawTypeCode()));

        List<Player> players = getPlayers(draw);
        players.forEach(player -> player.setTour(params.getTour()));

        return new MatchCollectResult(
                draw.getDiscipline(),
                draw.getDrawTypeCode(),
                draw.getMeta(),
                draw.getTournamentId(),
                draw.getYear(),
                matches,
                players,
                getEntries(draw)
        );
    }

    /** 调用 Client 获取原始数据，供各 draw 方法共享 */
    protected R request(DrawParams params) { return null; }

    protected List<DrawResult<S>> ms(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> md(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> ls(R data, DrawParams params) { return List.of(); }

    protected List<DrawResult<S>> ld(R data, DrawParams params) { return List.of(); }

    /**
     * 校验上游响应是否属于本次请求的赛事，防止缓存或代理返回其他赛事数据后被错误落库。
     * eventId 按数字比较，以兼容请求参数中的前导零（如 "0404"）。
     */
    protected boolean isRequestedEvent(Integer responseEventId, Integer responseYear, DrawParams params) {
        if (responseEventId == null || responseYear == null || params == null
                || params.getTournamentId() == null) {
            return false;
        }
        try {
            return responseEventId.equals(Integer.valueOf(params.getTournamentId()))
                    && responseYear.equals(params.getYear());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 从切片提取所有比赛 */
    public abstract List<Match> getMatches(DrawResult<S> draw, String tournamentId);

    /** 从切片提取所有参赛球员 */
    public abstract List<Player> getPlayers(DrawResult<S> draw);

    /** 从切片提取参赛资格/种子信息 */
    public abstract List<TournamentEntry> getEntries(DrawResult<S> draw);

    /** 标识当前 Client 对应的采集类型，用于 Manager 构建路由 Map */
    public abstract CollectType collectType();

    /**
     * 从 matchId 中提取数字部分作为 matchIndex
     * 例如 "LS1234" -> 1234, "LD5678" -> 5678
     */
    protected Integer parseMatchIndex(String matchId) {
        if (matchId == null || matchId.isEmpty()) return null;
        String digits = matchId.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
