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
     * 根据 bizId 列表批量查询
     */
    List<MeetupData> findByBizIds(List<String> bizIds);

    /**
     * 根据城市编码和状态列表查询
     */
    List<MeetupData> findByCityCodeAndStatus(String cityCode, List<String> statusList);

    /**
     * 更新状态
     */
    void updateStatus(String bizId, String status);

    /**
     * 原子自增人数（报名/审批通过）
     * @return 影响行数，0 表示已满或状态变化
     */
    int incrementPlayers(String bizId);

    /**
     * 原子自减人数（退出/拒绝）
     * @return 影响行数
     */
    int decrementPlayers(String bizId);

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
     * 判断用户是否为该场参与者（发布者 + 已批准报名者）
     */
    boolean isParticipant(String meetupId, String userId);

    /**
     * 获取该场全部参与者 userId 列表（发布者 + 已批准报名者）
     */
    List<String> listParticipantUserIds(String meetupId);

    /**
     * 懒判定约球是否已结束：end_time < NOW()
     */
    boolean isFinished(String meetupId);

    /**
     * 统计用户近 N 天内完成的约球场数（可信度计算用）
     * @param userId 用户 ID
     * @param days 近 N 天
     * @return 完成场数
     */
    long countFinishedMatches(String userId, int days);

    /**
     * 查询可报名的约球列表（带筛选、排序、分页）
     * @param param 查询参数
     * @return 分页结果
     */
    PageDTO<MeetupData> listAvailable(MeetupListQueryParam param);

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
}
