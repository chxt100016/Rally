package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rally.db.meetup.entity.MeetupPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MeetupMapper extends BaseMapper<MeetupPO> {

    /** 用户维度约球列表（IN_PROGRESS / COMPLETED / MY_PUBLISH），searchAfter 游标分页 */
    List<MeetupPO> listByUserFilter(@Param("param") com.rally.domain.meetup.model.MeetupListQueryParam param);

    /** 创建人有待审批报名的约球 */
    IPage<MeetupPO> listCreatorPending(Page<MeetupPO> page, @Param("creatorId") String creatorId);

    /** PENDING tab：创建人有待审批 + 参与者已结束未录比分，UNION searchAfter 游标分页 */
    List<MeetupPO> listPendingMeetups(@Param("userId") String userId, @Param("deadlineDays") int deadlineDays, @Param("lastId") String lastId, @Param("limit") int limit, @Param("participatedStatuses") List<String> participatedStatuses);

    /** RECENT tab：用户为创建人或已批准报名的约球，不限状态，searchAfter 游标分页 */
    List<MeetupPO> listRecentByUser(@Param("userId") String userId, @Param("lastId") String lastId, @Param("limit") int limit, @Param("participatedStatuses") List<String> participatedStatuses);
}
