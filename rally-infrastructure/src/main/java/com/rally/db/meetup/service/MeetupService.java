package com.rally.db.meetup.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.mapper.MeetupMapper;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeetupService extends ServiceImpl<MeetupMapper, MeetupPO> {

    /** 用户维度约球列表（IN_PROGRESS / COMPLETED / MY_PUBLISH），searchAfter 游标分页 */
    public List<MeetupPO> listByUserFilter(MeetupListQueryParam param) {
        return baseMapper.listByUserFilter(param);
    }

    /** PENDING tab：UNION searchAfter 游标分页 */
    public List<MeetupPO> listPendingMeetups(String userId, int deadlineDays, String lastId, int limit, List<String> participatedStatuses) {
        return baseMapper.listPendingMeetups(userId, deadlineDays, lastId, limit, participatedStatuses);
    }

    /** RECENT tab：用户为创建人或已批准报名的约球，不限状态，searchAfter 游标分页 */
    public List<MeetupPO> listRecentByUser(String userId, String lastId, int limit, List<String> participatedStatuses) {
        return baseMapper.listRecentByUser(userId, lastId, limit, participatedStatuses);
    }

    /**
     * 批量更新状态为 FINISHED（兜底任务用）
     * @return 影响行数
     */
    public int batchUpdateToFinished() {
        return baseMapper.update(null, new LambdaUpdateWrapper<MeetupPO>()
                .in(MeetupPO::getStatus, "OPEN", "full")
                .lt(MeetupPO::getEndTime, LocalDateTime.now())
                .set(MeetupPO::getStatus, "FINISHED"));
    }
}
