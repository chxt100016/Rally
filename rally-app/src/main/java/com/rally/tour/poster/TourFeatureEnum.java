package com.rally.tour.poster;

/**
 * 赛事特色素材库（可拔插，城市特色与中央球场特色共用一张表）。
 * <p>
 * 一个赛事只会命中一种特色，故合并为单一枚举，按级别决定查法（见 {@link TourLevelEnum}）：
 * <ul>
 *   <li>1000 以下（250 / 500）：中央球场无辨识度，用<b>举办城市</b>特色作远景点缀。
 *       用 city 匹配 {@link #fromCity}，desc 给可视化的<b>轻量地标关键词</b>（不报专有名词），避免地标塞满天际线抢占视角。</li>
 *   <li>1000 及以上（1000 / 大满贯 / 年终总决赛）：中央球场本身即标志性符号，突出<b>中央球场</b>建筑特色。
 *       用 tournamentId 匹配 {@link #fromTournamentId}，desc 给可视化的<b>建筑特征描述</b>（不报专有名词，只给可画出的特征，如「可开合顶棚、红土碗状环形看台」），避免生图模型认错球场。</li>
 * </ul>
 * code 为匹配键：低级别填城市名（小写），高级别填 tournamentId。
 * <p>
 * TODO 素材待下一步填充。
 */
public enum TourFeatureEnum {
    ;

    private final String code;
    private final String desc;

    TourFeatureEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 城市特色查找（1000 以下用）。未命中返回 null。 */
    public static TourFeatureEnum fromCity(String city) {
        return match(city == null ? null : city.trim().toLowerCase());
    }

    /** 中央球场特色查找（1000 及以上用）。未命中返回 null。 */
    public static TourFeatureEnum fromTournamentId(String tournamentId) {
        return match(tournamentId == null ? null : tournamentId.trim());
    }

    private static TourFeatureEnum match(String key) {
        if (key == null || key.isBlank()) return null;
        for (TourFeatureEnum feature : values()) {
            if (feature.code.equals(key)) return feature;
        }
        return null;
    }
}
