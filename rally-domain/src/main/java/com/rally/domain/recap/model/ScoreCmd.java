package com.rally.domain.recap.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;

public interface ScoreCmd {
    Integer getSetNum();
    SetFormatEnum getSetFormatType();
    MatchTypeEnum getMatchType();
    String getSideAPlayer1();
    String getSideAPlayer2();
    String getSideBPlayer1();
    String getSideBPlayer2();
    Integer getSideAScore();
    Integer getSideBScore();
    Integer getSideATiebreakScore();
    Integer getSideBTiebreakScore();
}
