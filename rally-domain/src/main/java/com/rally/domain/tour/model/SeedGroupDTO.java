package com.rally.domain.tour.model;

import lombok.Data;

import java.util.List;

@Data
public class SeedGroupDTO {
    private SeedGroupTypeEnum type;
    private List<SeedVO> data;
}
