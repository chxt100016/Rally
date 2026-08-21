package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 赛事落地页主题配置。
 */
@Data
public class TournamentThemeConfig {

    private static final String HEX_COLOR_PATTERN = "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$";

    /** 主操作按钮颜色，支持 #RGB/#RGBA/#RRGGBB/#RRGGBBAA。 */
    @NotBlank(message = "请填写赛事按钮颜色")
    @Pattern(regexp = HEX_COLOR_PATTERN, message = "赛事按钮颜色格式不正确")
    private String buttonColor;

    /** 赛事落地页背景色，支持 #RGB/#RGBA/#RRGGBB/#RRGGBBAA。 */
    @NotBlank(message = "请填写赛事背景颜色")
    @Pattern(regexp = HEX_COLOR_PATTERN, message = "赛事背景颜色格式不正确")
    private String backgroundColor;
}
