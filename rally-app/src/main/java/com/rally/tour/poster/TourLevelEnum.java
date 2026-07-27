package com.rally.tour.poster;

/**
 * 赛事级别模块（可拔插，核心）。
 * <p>
 * 通过「相机视角高度」区分级别：级别越高越俯视，一眼看出赛事规模。
 * 同时绑定与级别匹配的「光线氛围」与「观众规模」。
 * <p>
 * 特色元素归属规则（{@link #useCenterCourtFeature}）：
 * <ul>
 *   <li>1000 以下（250 / 500）：中央球场无辨识度，改用<b>举办城市</b>特色作远景点缀（{@link TourFeatureEnum#fromCity}）。</li>
 *   <li>1000 及以上（1000 / 大满贯 / 年终总决赛）：中央球场本身即标志性符号，突出<b>中央球场</b>建筑特色（{@link TourFeatureEnum#fromTournamentId}）。</li>
 * </ul>
 *
 * @param code                 匹配 category 原始值（大写比较）
 * @param label                展示用级别名
 * @param viewpoint            视角高度描述（区分级别的核心信号）
 * @param lighting             光线氛围描述（配合级别叠加）
 * @param crowd                观众规模描述（强绑定级别）
 * @param useCenterCourtFeature true=用中央球场特色，false=用城市特色
 */
public enum TourLevelEnum {

    L250("250", "250", false,
            "相机位于场边略低机位，接近平视微微仰拍，看台在两侧不高，亲切近距离的视角。",
            "自然日光，明亮清爽，轻松氛围。",
            "看台观众稀疏，零星坐着，休闲气氛。"),

    L500("500", "500", false,
            "相机位于中层看台高度，约30度向下俯拍，能看到大半个球场，两侧看台完整，正式赛事视角。",
            "黄昏暖光或明亮球场灯，层次分明。",
            "看台约半满，观众成片，有赛事热度。"),

    L1000("1000", "1000", true,
            "相机位于高层看台，约45度俯视，球场完整轮廓尽收眼底，环绕的看台形成气势，大型赛事的恢弘视角。",
            "戏剧性侧光，冷暖对比，大赛紧张感，天空渐变。",
            "看台大部分坐满，人群密集。"),

    GS("GS", "大满贯", true,
            "相机高空俯瞰，约60度大俯视角，球场居中被环形大看台完整包裹，建筑宏伟，神圣殿堂般的庄严视角。",
            "金色黄昏或壮丽夜景灯光，宏大史诗感，光晕效果。",
            "看台座无虚席，人山人海，盛大满场。"),

    FINALS("FINALS", "年终总决赛", true,
            "相机近乎正上方鸟瞰（顶视约75度），完整封闭的室内球馆，球场如舞台被环形看台与穹顶包围，聚光灯汇聚场心，终极殿堂剧场感。",
            "全暗环境中聚光灯打亮球场，蓝紫色科技冷调，光束穿过空间，极致仪式感。",
            "看台座无虚席，人山人海，盛大满场。");

    private final String code;
    private final String label;
    private final boolean useCenterCourtFeature;
    private final String viewpoint;
    private final String lighting;
    private final String crowd;

    TourLevelEnum(String code, String label, boolean useCenterCourtFeature, String viewpoint, String lighting, String crowd) {
        this.code = code;
        this.label = label;
        this.useCenterCourtFeature = useCenterCourtFeature;
        this.viewpoint = viewpoint;
        this.lighting = lighting;
        this.crowd = crowd;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean isUseCenterCourtFeature() {
        return useCenterCourtFeature;
    }

    public String getViewpoint() {
        return viewpoint;
    }

    public String getLighting() {
        return lighting;
    }

    public String getCrowd() {
        return crowd;
    }

    /** 未命中返回 null，由调用方决定兜底。FINAL/FINALS 归一到年终总决赛。 */
    public static TourLevelEnum fromCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toUpperCase();
        if ("FINAL".equals(normalized)) return FINALS;
        for (TourLevelEnum level : values()) {
            if (level.code.equals(normalized)) return level;
        }
        return null;
    }
}
