package com.rally.domain.tour.tournament;

/**
 * {@code tour_tournament} 的唯一领域写端口。
 *
 * <p>适配器的名录更新语句必须排除 {@code image_path/background_path}；图片更新
 * 则只能在同一条语句中写这两个字段。自然键冲突必须作为显式插入结果返回。</p>
 */
public interface TourTournamentPersistence {

    /** 按 tournament_id+year 读取，不存在时返回 {@code null}。 */
    TourTournamentState findByIdentity(TourTournamentIdentity identity);

    /** 插入一条已经校验且图片绑定为空的新赛事年度。 */
    TourTournamentInsertResult insert(TourTournamentState state);

    /** 整体刷新来源主档，SQL 不得包含两个图片列。 */
    boolean replaceCatalogProfile(long id, TourTournamentProfile profile);

    /** 原子成对替换两个图片列，不得写任何名录字段。 */
    boolean replaceImageBinding(long id, TourTournamentImageBinding binding);
}
