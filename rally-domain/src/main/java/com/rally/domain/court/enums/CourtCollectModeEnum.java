package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 球场抓取模式
 */
@AllArgsConstructor
@Getter
public enum CourtCollectModeEnum {
    /** 全量覆盖：已收录的按本次结果改写，未收录的新增 */
    FULL("全量覆盖"),
    /** 增量：只新增未收录的，已收录的一概不动 */
    INCREMENT("增量");

    public final String label;
}
