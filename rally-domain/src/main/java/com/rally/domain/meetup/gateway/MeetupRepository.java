package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.meetup.model.PageDTO;

import java.util.List;

/**
 * 约球主表读写网关接口
 */
public interface MeetupRepository {
    /**
     * 保存约球（新增或更新）
     */
    void save(MeetupData data);

    /**
     * 一次性保存整个约球聚合根（约球数据 + 报名记录）
     */
    void save(Meetup meetup);

    /**
     * 根据 bizId 查询
     */
    MeetupData findByBizId(String bizId);



    /**
     * 统计用户当日活跃发布数（status=OPEN）
     */
    long countTodayActive(String userId);

    /**
     * 批量更新状态为 FINISHED（兜底任务用）
     * @return 影响行数
     */
    int batchUpdateToFinished();



    /**
     * 统计用户近 N 天内完成的约球场数（可信度计算用）
     * @param userId 用户 ID
     * @param days 近 N 天
     * @return 完成场数
     */
    long countFinishedMatches(String userId, int days);

    /**
     * 查询可报名的约球列表（带筛选、排序、searchAfter 游标分页）
     * 返回 pageSize+1 条数据，由调用方判断是否还有下一页
     * @param param 查询参数
     * @return 约球数据列表
     */
    List<MeetupData> listAvailable(MeetupListQueryParam param);

    /**
     * 距离排序查询（不分页）：SQL 用 ST_Distance_Sphere 计算经纬度距离、范围过滤并按距离升序
     * 返回按距离排好序的全量数据，距离（米）已写入 distanceMeters，由调用方做内存游标分页
     * @param param 查询参数（需含 lng/lat，radiusMeters 可空表示不限范围）
     * @return 按距离升序的结果列表
     */
    List<MeetupData> listByDistance(MeetupListQueryParam param);

    /**
     * IN_PROGRESS tab：参与过 且 status=OPEN 且未结束，searchAfter 游标分页
     * @param param 查询参数（userId + statusList + registrationStatuses + lastId + limit）
     * @return 分页结果
     */
    PageDTO<MeetupData> listInProgress(MeetupListQueryParam param);

    /**
     * COMPLETED tab：参与过 且 (FINISHED 或懒判定已结束)，searchAfter 游标分页
     * @param param 查询参数（userId + registrationStatuses + lastId + limit）
     * @return 分页结果
     */
    PageDTO<MeetupData> listCompleted(MeetupListQueryParam param);

    /**
     * 统计用户已完成的约球数（与 listCompleted 同条件）
     * @param param 查询参数（userId + registrationStatuses）
     * @return 已完成数量
     */
    long countCompleted(MeetupListQueryParam param);

    /**
     * MY_PUBLISH tab：创建人是当前用户，searchAfter 游标分页
     * @param param 查询参数（creatorId + lastId + limit）
     * @return 分页结果
     */
    PageDTO<MeetupData> listMyPublish(MeetupListQueryParam param);

    /**
     * 统计用户发布的约球数（与 listMyPublish 同条件）
     * @param param 查询参数（creatorId）
     * @return 发布数量
     */
    long countMyPublish(MeetupListQueryParam param);

    /**
     * PENDING tab：创建人有待审批 + 参与者已结束未录比分，UNION searchAfter 游标分页
     * @param userId 当前用户 ID
     * @param deadlineDays review deadline 天数
     * @param lastId 上一页最后一条的 bizId
     * @param limit 查询条数（size + 1）
     * @return 分页结果
     */
    PageDTO<MeetupData> listPendingMeetups(String userId, int deadlineDays, String lastId, int limit);

    /**
     * RECENT tab：用户为创建人或已批准报名的约球，不限状态，searchAfter 游标分页
     * @param userId 当前用户 ID
     * @param lastId 上一页最后一条的 bizId
     * @param limit 查询条数（size + 1）
     * @return 分页结果
     */
    PageDTO<MeetupData> listRecentByUser(String userId, String lastId, int limit);
}
