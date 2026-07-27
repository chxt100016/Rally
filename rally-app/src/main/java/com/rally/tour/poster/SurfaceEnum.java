package com.rally.tour.poster;

/**
 * 场地材质模块（可拔插）。
 * code 用于匹配 {@link com.rally.domain.tour.model.TournamentData#getSurface()} 的原始值（小写）。
 */
public enum SurfaceEnum {

    HARD("hard", "深蓝色球场搭配绿色外场，清晰的白色边线，现代硬地质感。"),
    CLAY("clay", "橙红色红土球场，细腻的土粒质感，白色边线略带土色磨损。"),
    GRASS("grass", "翠绿色天然草地球场，修剪整齐的草纹，经典白色边线。"),
    INDOOR_HARD("indoor hard", "室内蓝色硬地球场，深色背景衬托，人工照明下的地面反光。"),
    INDOOR_CLAY("indoor clay", "室内红土球场，暖色灯光打在橙红土面上，封闭空间氛围。"),
    /** 兜底：仅标注 indoor 未细分材质时按室内硬地处理 */
    INDOOR("indoor", "室内蓝色硬地球场，深色背景衬托，人工照明下的地面反光。");

    private final String code;
    private final String desc;

    SurfaceEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 未命中时返回 null，由调用方决定兜底文案。 */
    public static SurfaceEnum fromCode(String rawSurface) {
        if (rawSurface == null || rawSurface.isBlank()) return null;
        String normalized = rawSurface.trim().toLowerCase();
        for (SurfaceEnum surface : values()) {
            if (surface.code.equals(normalized)) return surface;
        }
        return null;
    }
}
