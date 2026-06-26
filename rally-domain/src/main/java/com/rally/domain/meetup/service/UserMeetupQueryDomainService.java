package com.rally.domain.meetup.service;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户维度约球查询领域服务
 * 负责"我的约球"五个 Tab（待处理/进行中/我发布/已完成/最近）的查询核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMeetupQueryDomainService {

    private final MeetupRepository meetupRepository;

    // ======================== 五个 Tab 分支 ========================

    /**
     * 待处理：创建人有 pending 报名待审批 + 参参与者已结束但未录比分 + 有未读消息
     * UNION SQL searchAfter 游标分页，review deadline 过滤在 SQL 中完成
     */
    public PageDTO<MeetupData> listPending(String userId, String lastId, int limit) {
        int deadlineDays = SystemConfig.getInt(SystemConfigKey.REVIEW_DEADLINE_DAYS.getKey());
        return meetupRepository.listPendingMeetups(userId, deadlineDays, lastId, limit);
    }

    /**
     * 进行中：我创建或我已批准参与 + status=OPEN + 未到结束时间
     */
    public PageDTO<MeetupData> listInProgress(String userId, String lastId, int limit) {
        MeetupListQueryParam param = MeetupListQueryParam.builder()
                .userId(userId).statusList(List.of("OPEN"))
                .registrationStatuses(RegistrationStatusEnum.getParticipated())
                .lastId(lastId).limit(limit).build();
        return meetupRepository.listInProgress(param);
    }

    /**
     * 我发布：创建人是当前用户，按创建时间倒序
     */
    public PageDTO<MeetupData> listMyPublish(String userId, String lastId, int limit) {
        MeetupListQueryParam param = MeetupListQueryParam.builder()
                .creatorId(userId)
                .lastId(lastId).limit(limit).build();
        return meetupRepository.listMyPublish(param);
    }

    /**
     * 已完成：报名状态为完成态（REVIEWED/SKIPPED）即可，不判断 meetup 状态与时间
     */
    public PageDTO<MeetupData> listCompleted(String userId, String lastId, int limit) {
        return meetupRepository.listCompleted(buildCompletedParam(userId).lastId(lastId).limit(limit).build());
    }

    /**
     * 最近：用户为创建人或已批准报名的约球，不限状态（球员主页用）
     */
    public PageDTO<MeetupData> listRecent(String userId, String lastId, int limit) {
        return meetupRepository.listRecentByUser(userId, lastId, limit);
    }

    // ======================== 统计（与对应 list 同条件，仅 count） ========================

    /**
     * 统计用户已完成的约球数（与 listCompleted 同条件）
     */
    public long countCompleted(String userId) {
        return meetupRepository.countCompleted(buildCompletedParam(userId).build());
    }

    /**
     * 统计用户发布的约球数（与 listMyPublish 同条件）
     */
    public long countMyPublish(String creatorId) {
        return meetupRepository.countMyPublish(MeetupListQueryParam.builder().creatorId(creatorId).build());
    }

    // ======================== 内部工具方法 ========================

    /** 已完成查询参数：参与过 + 参与状态，list 与 count 共用 */
    private MeetupListQueryParam.MeetupListQueryParamBuilder buildCompletedParam(String userId) {
        return MeetupListQueryParam.builder()
                .userId(userId)
                .registrationStatuses(RegistrationStatusEnum.getCompleted());
    }

}
