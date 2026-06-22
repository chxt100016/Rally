package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

@Data
public class MatchGroupDTO {
    private String name;
    private List<MatchQueryVO> data;
}
