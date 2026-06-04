package com.rally.domain.score.gateway;

import java.util.List;

/**
 * 批量评分幂等状态网关接口
 */
public interface ScoreStatusGateway {

    /**
     * 查询待处理的 meetup_id 列表
     * 条件：rally_meetup.status=finished 且
     *       (rally_meetup_score_status.processed_at IS NULL OR processed_version < score_version)
     */
    List<String> findPendingMeetupIds();

    /**
     * 增加 score_version（评价/比分变更时调用）
     * 无行则 INSERT（version=1），有行则 version + 1
     */
    void bumpVersion(String meetupId);

    /**
     * 标记处理完成：processed_version = score_version, processed_at = NOW()
     */
    void markProcessed(String meetupId);
}
