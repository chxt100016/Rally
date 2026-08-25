package com.rally.court.model;

import lombok.Data;

/**
 * 球场写库统计，upsert-city-courts 活动的产出。
 */
@Data
public class UpsertResult {
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
}
