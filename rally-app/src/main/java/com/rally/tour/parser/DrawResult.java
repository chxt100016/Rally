package com.rally.tour.parser;

import com.rally.tour.model.Discipline;
import lombok.Value;

@Value
public class DrawResult<S> {
    S slice;
    Discipline discipline;
    /** 数据库中使用的签表类型代码，如 "MS"、"MD"、"LS" */
    String drawTypeCode;
    DrawMeta meta;
    String tournamentId;
    int year;
}
