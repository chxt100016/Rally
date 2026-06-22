package com.rally.domain.tennis.model;

import lombok.Data;

import java.util.List;

@Data
public class SeedGroupDTO {
    private String type;
    private List<SeedVO> data;
}
