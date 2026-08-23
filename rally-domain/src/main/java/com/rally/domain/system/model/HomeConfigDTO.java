package com.rally.domain.system.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 运营后台首页配置集合。 */
@Data
@AllArgsConstructor
public class HomeConfigDTO {
    private List<HomeConfigItemDTO> configs;
}
