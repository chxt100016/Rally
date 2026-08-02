package com.rally.tour.poster;

import com.rally.domain.tour.model.TournamentData;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * 海报生图提示词组装器（模块可拔插）。
 * <p>
 * 拼接顺序：风格块 → 基础占位 → 场地材质 → 视角级别 → 光线氛围 → 观众规模 → 特色块 → 收尾约束。
 * 特色块按级别二选一：1000 以下用城市特色，1000 及以上用中央球场特色（见 {@link TourLevelEnum}）。
 */
public class PosterPromptBuilder {

    private static final String TAIL = "球场正中央为空，不出现任何球员或人物；画面聚焦球场与看台建筑本身；"
            + "海报预留文字排版空间；图片比例16:9；高质量，细节丰富，专业体育海报。";

    private PosterPromptBuilder() {
    }

    public static String build(TournamentData data, PosterStyleEnum style) {
        String city = data.getCity() != null ? data.getCity().trim() : "";
        String name = buildTournamentName(data);

        TourLevelEnum level = TourLevelEnum.fromCategory(data.getCategory());
        SurfaceEnum surface = SurfaceEnum.fromCode(data.getSurface());

        StringBuilder sb = new StringBuilder();
        sb.append(style.getDesc());
        sb.append("一张").append(StringUtils.isNotBlank(city) ? city : "")
                .append(StringUtils.isNotBlank(name) ? name : "网球赛事")
                .append("宣传海报，画面中心是一片专业网球场。");

        if (surface != null) {
            sb.append(surface.getDesc());
        }
        if (level != null) {
            sb.append(level.getViewpoint());
            sb.append(level.getLighting());
            sb.append(level.getCrowd());
            sb.append(featureClause(data, level, city));
        }
        sb.append(TAIL);
        return sb.toString();
    }

    private static String buildTournamentName(TournamentData data) {
        return Stream.of(data.getName(), "tennis open", data.getTour(), data.getCategory())
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
    }

    /** 特色块：按级别二选一。素材库未命中时退化为通用文案。 */
    private static String featureClause(TournamentData data, TourLevelEnum level, String city) {
        if (level.isUseCenterCourtFeature()) {
            TourFeatureEnum feature = TourFeatureEnum.fromTournamentId(data.getTournamentId());
            if (feature != null) {
                return "着重还原中央球场的标志性建筑特征（" + feature.getDesc() + "），把这座中央球场独有的辨识度作为画面记忆点。";
            }
            return "着重刻画中央球场标志性的建筑轮廓与看台造型，作为画面记忆点。";
        }
        TourFeatureEnum feature = TourFeatureEnum.fromCity(city);
        if (feature != null) {
            return "在远景自然融入" + city + "的城市特色（" + feature.getDesc() + "），不突兀、不遮挡球场主体。";
        }
        if (StringUtils.isNotBlank(city)) {
            return "在远景自然融入" + city + "的城市气质作点缀，不突兀、不遮挡球场主体。";
        }
        return "";
    }
}
