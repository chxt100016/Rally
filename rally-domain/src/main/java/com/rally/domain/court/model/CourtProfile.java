package com.rally.domain.court.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 球场展示资料值对象，落在 rally_court.ext_data 的 JSON 里。
 * 拼音两项由聚合按球场名称自行维护，不由外部传入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtProfile {
    private String rating;
    private String cost;
    private String opentime;
    private String tel;
}
