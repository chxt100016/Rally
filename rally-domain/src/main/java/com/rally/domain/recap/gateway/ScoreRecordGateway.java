package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ScoreRecordData;

import java.util.List;

/**
 * 比分记录读写网关接口（含乐观锁）
 */
public interface ScoreRecordGateway {

    /**
     * 新增盘记录（INSERT，version=0）
     */
    void insert(ScoreRecordData data);

    /**
     * 乐观锁更新盘记录（UPDATE WHERE biz_id=? AND version=?）
     * @return 影响行数，0 表示版本冲突
     */
    int updateWithLock(ScoreRecordData data);

    /**
     * 根据 bizId 查询
     */
    ScoreRecordData findByBizId(String bizId);

    /**
     * 查询某场全部比分（按 set_number 升序）
     */
    List<ScoreRecordData> listByMeetupId(String rallyMeetupId);

    /**
     * 乐观锁删除盘记录（DELETE WHERE biz_id=? AND version=?）
     * @return 影响行数，0 表示版本冲突
     */
    int deleteByBizIdWithLock(String bizId, Integer version);
}
