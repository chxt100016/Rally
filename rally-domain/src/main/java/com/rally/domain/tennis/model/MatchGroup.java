package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

/**
 * 比赛分组领域对象（按球场或轮次聚合）
 */
@Data
public class MatchGroup {
    private String name;
    private List<MatchQueryVO> matches;
}
