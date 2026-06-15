package com.rally.domain.recap.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 删除比分命令（一次删除一盘，按 bizId 定位、天然防 ABA）
 */
@Data
public class ScoreDeleteCmd {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 目标盘记录 biz_id（雪花，永不复用，删除即用户当初看到的那条） */
    @NotBlank(message = "比分记录ID不能为空")
    private String bizId;
}
