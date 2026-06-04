package com.rally.domain.review.model;

import com.rally.domain.review.enums.SetFormatEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 记/改比分入参（按盘）
 */
@Data
public class ScoreRecordCmd {
    /** 盘记录 biz_id，为空表示新增 */
    private String bizId;

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String rallyMeetupId;

    /** 第几盘 */
    @NotNull(message = "盘号不能为空")
    @Min(value = 1, message = "盘号从1开始")
    private Integer setNumber;

    /** 赛制 */
    @NotNull(message = "请选择赛制")
    private SetFormatEnum setFormat;

    /** A 侧选手1 user_id */
    @NotBlank(message = "A侧选手1不能为空")
    private String sideAPlayer1;

    /** A 侧选手2 user_id（单打为 null） */
    private String sideAPlayer2;

    /** B 侧选手1 user_id */
    @NotBlank(message = "B侧选手1不能为空")
    private String sideBPlayer1;

    /** B 侧选手2 user_id（单打为 null） */
    private String sideBPlayer2;

    /** A 侧本盘比分 */
    @NotNull(message = "A侧比分不能为空")
    @Min(value = 0, message = "比分不能为负")
    private Integer sideAScore;

    /** B 侧本盘比分 */
    @NotNull(message = "B侧比分不能为空")
    @Min(value = 0, message = "比分不能为负")
    private Integer sideBScore;

    /** 乐观锁版本号，新增传 null，修改必传 */
    private Integer version;
}
