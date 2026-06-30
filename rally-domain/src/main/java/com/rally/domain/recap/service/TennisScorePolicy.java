package com.rally.domain.recap.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.utils.Assert;

public class TennisScorePolicy {

    private TennisScorePolicy() {}

    /**
     * 根据主分计算获胜边，分数大的一边赢。主分相同无法判断胜负，抛出异常。
     */
    public static String calcWinSide(int sideAScore, int sideBScore) {
        Assert.isTrue(sideAScore != sideBScore, BizErrorCode.INVALID_WIN_SIDE);
        return sideAScore > sideBScore ? "A" : "B";
    }
}
