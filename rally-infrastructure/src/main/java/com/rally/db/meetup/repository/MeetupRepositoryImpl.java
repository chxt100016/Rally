package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.service.MeetupService;
import com.rally.db.meetup.service.RegistrationService;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.meetup.model.PageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 约球主表 Repository 实现
 */
@Component
@RequiredArgsConstructor
public class MeetupRepositoryImpl implements MeetupRepository {

    private final MeetupService meetupService;
    private final RegistrationService registrationService;
    private static final MeetupConvertMapper MAPPER = MeetupConvertMapper.INSTANCE;

    @Override
    public void save(MeetupData data) {
        saveOrUpdateByBizId(MAPPER.toMeetupPO(data));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Meetup meetup) {
        // 聚合根计算 currentPlayers 后整体落库：主表 + 报名表均按 bizId upsert
        MeetupData data = meetup.getData();
        data.setCurrentPlayers(meetup.countApprovedPlayers());
        save(data);
        meetup.getRegistrations().forEach(reg -> saveRegistrationByBizId(MAPPER.toRegistrationPO(reg)));
    }

    @Override
    public MeetupData findByBizId(String bizId) {
        MeetupPO po = meetupService.lambdaQuery()
                .eq(MeetupPO::getBizId, bizId)
                .one();
        return MAPPER.toMeetupData(po);
    }

    @Override
    public long countTodayActive(String userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .in(MeetupPO::getStatus, "OPEN", "full")
                .ge(MeetupPO::getCreateTime, todayStart)
                .count();
    }

    @Override
    public List<String> listActiveIds(String cityCode) {
        return meetupService.lambdaQuery()
                .select(MeetupPO::getBizId)
                .eq(MeetupPO::getCityCode, cityCode)
                .in(MeetupPO::getStatus, "OPEN", "full")
                .gt(MeetupPO::getEndTime, LocalDateTime.now())
                .list()
                .stream()
                .map(MeetupPO::getBizId)
                .toList();
    }

    @Override
    public int batchUpdateToFinished() {
        return meetupService.batchUpdateToFinished();
    }

    @Override
    public long countFinishedMatches(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        // 统计用户作为发布者的已完成约球
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .eq(MeetupPO::getStatus, "FINISHED")
                .ge(MeetupPO::getEndTime, since)
                .count();
        // TODO: 还需要统计用户作为报名者（已批准）的已完成约球，需要关联 waitlist 表
    }

    @Override
    public List<MeetupData> listAvailable(MeetupListQueryParam param) {
        LambdaQueryWrapper<MeetupPO> wrapper = baseFilterWrapper(param);
        // searchAfter 复合游标：(startTime, bizId) 升序，取游标之后的数据
        if (param.getLastStartTime() != null) {
            LocalDateTime lastStart = param.getLastStartTime();
            String lastBizId = param.getLastBizId();
            wrapper.and(w -> w.gt(MeetupPO::getStartTime, lastStart).or(o -> o.eq(MeetupPO::getStartTime, lastStart).gt(MeetupPO::getBizId, lastBizId)));
        }
        wrapper.orderByAsc(MeetupPO::getStartTime).orderByAsc(MeetupPO::getBizId).last("LIMIT " + (param.getPageSize() + 1));
        return MAPPER.toMeetupDataList(meetupService.list(wrapper));
    }

    @Override
    public List<MeetupData> listByMeetupIdsWithFilter(MeetupListQueryParam param) {
        // 不分页，返回所有符合筛选条件的结果
        return MAPPER.toMeetupDataList(meetupService.list(baseFilterWrapper(param)));
    }

    @Override
    public long countByCreatorId(String userId) {
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .count();
    }

    @Override
    public long countFinishedByCreatorId(String userId) {
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .eq(MeetupPO::getStatus, "FINISHED")
                .count();
    }

    @Override
    public PageDTO<MeetupData> listByUserFilter(MeetupListQueryParam param) {
        return toPage(meetupService.listByUserFilter(param), param.getLimit());
    }

    @Override
    public PageDTO<MeetupData> listPendingMeetups(String userId, int deadlineDays, String lastId, int limit) {
        return toPage(meetupService.listPendingMeetups(userId, deadlineDays, lastId, limit), limit);
    }

    @Override
    public PageDTO<MeetupData> listRecentByUser(String userId, String lastId, int limit) {
        return toPage(meetupService.listRecentByUser(userId, lastId, limit), limit);
    }

    /**
     * 城市 + 状态 + 未结束 + 类型 + 时间范围 + 水平交集 的基础筛选条件
     */
    private LambdaQueryWrapper<MeetupPO> baseFilterWrapper(MeetupListQueryParam param) {
        LambdaQueryWrapper<MeetupPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetupPO::getCityCode, param.getCityCode())
                .in(MeetupPO::getStatus, "OPEN", "FULL")
                .gt(MeetupPO::getEndTime, LocalDateTime.now());
        if (!CollectionUtils.isEmpty(param.getMeetupIds())) {
            wrapper.in(MeetupPO::getBizId, param.getMeetupIds());
        }
        if (param.getMatchType() != null) {
            wrapper.eq(MeetupPO::getMatchType, param.getMatchType().name());
        }
        if (param.getStartTimeFrom() != null) {
            wrapper.ge(MeetupPO::getStartTime, param.getStartTimeFrom());
        }
        if (param.getStartTimeTo() != null) {
            wrapper.le(MeetupPO::getStartTime, param.getStartTimeTo());
        }
        if (param.getLevelMin() != null && param.getLevelMax() != null) {
            BigDecimal queryMin = param.getLevelMin();
            BigDecimal queryMax = param.getLevelMax();
            // 区间交集：约球 [level_min, level_max] 与查询 [queryMin, queryMax] 重叠，null 边界表示无限制
            wrapper.and(w -> w
                    .and(inner -> inner.isNull(MeetupPO::getLevelMin).or(le -> le.le(MeetupPO::getLevelMin, queryMax)))
                    .and(inner -> inner.isNull(MeetupPO::getLevelMax).or(ge -> ge.ge(MeetupPO::getLevelMax, queryMin))));
        }
        return wrapper;
    }

    /**
     * searchAfter 分页：多查了 1 条用于判断是否还有下一页
     */
    private PageDTO<MeetupData> toPage(List<MeetupPO> poList, int limit) {
        boolean hasMore = poList.size() > limit - 1;
        List<MeetupPO> pageData = hasMore ? poList.subList(0, limit - 1) : poList;
        return new PageDTO<>(MAPPER.toMeetupDataList(pageData), null, hasMore);
    }

    /**
     * 约球主表按 bizId upsert：存在则更新，不存在则新增
     */
    private void saveOrUpdateByBizId(MeetupPO po) {
        boolean updated = po.getBizId() != null && meetupService.lambdaUpdate().eq(MeetupPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            meetupService.save(po);
        }
    }

    /**
     * 报名表按 bizId upsert：存在则更新，不存在则新增
     */
    private void saveRegistrationByBizId(RegistrationPO po) {
        boolean updated = po.getBizId() != null && registrationService.lambdaUpdate().eq(RegistrationPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            registrationService.save(po);
        }
    }
}
