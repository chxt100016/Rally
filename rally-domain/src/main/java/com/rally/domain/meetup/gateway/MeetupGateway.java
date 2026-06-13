package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.meetup.model.PageDTO;

import java.util.List;

/**
 * 约球主表读写网关接口
 */
public interface MeetupGateway {
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
     * 统计用户当日活跃发布数（status IN OPEN,FULL）
     */
    long countTodayActive(String userId);

    /**
     * 查询城市下活跃约球 ID 列表（status IN OPEN,FULL AND end_time > NOW()）
     */
    List<String> listActiveIds(String cityCode);

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
     * 按 meetupId 列表 + 筛选条件查询（不分页，距离排序用）
     * @param param 查询参数（meetupIds 用于 IN 查询）
     * @return 符合筛选条件的结果列表
     */
    List<MeetupData> listByMeetupIdsWithFilter(MeetupListQueryParam param);

    /**
     * 统计用户发布的比赛次数
     * @param userId 用户 ID
     * @return 发布次数
     */
    long countByCreatorId(String userId);

    /**
     * 统计用户已完成的约球次数（status=FINISHED）
     */
    long countFinishedByCreatorId(String userId);

    /**
     * 查询用户相关的约球列表（按状态+参与关系筛选，分页）
     * 用于 IN_PROGRESS / COMPLETED / MY_PUBLISH tab
     * @param param 查询参数（含 creatorId 或 userId + statusList）
     * @return 分页结果
     */
    PageDTO<MeetupData> listByUserFilter(MeetupListQueryParam param);

    /**
     * PENDING tab：创建人有待审批 + 参与者已结束未录比分，UNION 分页
     * @param userId 当前用户 ID
     * @param deadlineDays review deadline 天数
     * @param pageNo 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageDTO<MeetupData> listPendingMeetups(String userId, int deadlineDays, int pageNo, int pageSize);

    /**
     * RECENT tab：用户为创建人或已批准报名的约球，不限状态，按开始时间倒序
     * @param userId 当前用户 ID
     * @param pageSize 数量
     * @return 分页结果
     */
    PageDTO<MeetupData> listRecentByUser(String userId, int pageSize);
}
