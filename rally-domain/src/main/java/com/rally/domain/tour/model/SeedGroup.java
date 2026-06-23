package com.rally.domain.tour.model;

import lombok.Data;

import java.util.List;

/**
 * 种子分组领域对象（ATP / WTA / OUT）
 */
@Data
public class SeedGroup {
    private String type;
    private List<SeedVO> seeds;
}
