package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rally.db.meetup.entity.MeetupPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MeetupMapper extends BaseMapper<MeetupPO> {

    /** 用户维度约球列表（IN_PROGRESS / COMPLETED / MY_PUBLISH） */
    IPage<MeetupPO> listByUserFilter(Page<MeetupPO> page, @Param("param") com.rally.domain.meetup.model.MeetupListQueryParam param);

    /** 创建人有待审批报名的约球 */
    IPage<MeetupPO> listCreatorPending(Page<MeetupPO> page, @Param("creatorId") String creatorId);

    /** PENDING tab：创建人有待审批 + 参与者已结束未录比分，UNION 分页 */
    IPage<MeetupPO> listPendingMeetups(Page<MeetupPO> page, @Param("userId") String userId, @Param("deadlineDays") int deadlineDays);

    /** RECENT tab：用户为创建人或已批准报名的约球，不限状态，按开始时间倒序 */
    IPage<MeetupPO> listRecentByUser(Page<MeetupPO> page, @Param("userId") String userId);
}
