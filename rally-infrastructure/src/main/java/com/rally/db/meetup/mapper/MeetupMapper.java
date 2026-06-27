package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MeetupMapper extends BaseMapper<MeetupPO> {

    /** 距离排序：SQL 用 ST_Distance_Sphere 计算距离、范围过滤并按距离升序，返回全量（量不大，不下推分页） */
    List<MeetupPO> listByDistance(@Param("param") MeetupListQueryParam param);

    /** IN_PROGRESS：参与过 且 status=OPEN 且未结束，searchAfter 游标分页 */
    List<MeetupPO> listInProgress(@Param("param") MeetupListQueryParam param);

    /** COMPLETED：参与过 且 (FINISHED 或懒判定已结束)，searchAfter 游标分页 */
    List<MeetupPO> listCompleted(@Param("param") MeetupListQueryParam param);

    /** COMPLETED 计数（与 listCompleted 同条件） */
    long countCompleted(@Param("param") MeetupListQueryParam param);

    /** MY_PUBLISH：创建人是当前用户，searchAfter 游标分页 */
    List<MeetupPO> listMyPublish(@Param("param") MeetupListQueryParam param);

    /** MY_PUBLISH 计数（与 listMyPublish 同条件） */
    long countMyPublish(@Param("param") MeetupListQueryParam param);

    /** PENDING tab：创建人有待审批 + 参与者已结束未录比分，UNION searchAfter 游标分页 */
    List<MeetupPO> listPendingMeetups(@Param("userId") String userId, @Param("deadlineDays") int deadlineDays, @Param("lastId") String lastId, @Param("limit") int limit, @Param("participatedStatuses") List<String> participatedStatuses);

    /** RECENT tab：用户为创建人或已批准报名的约球，不限状态，searchAfter 游标分页 */
    List<MeetupPO> listRecentByUser(@Param("userId") String userId, @Param("lastId") String lastId, @Param("limit") int limit, @Param("participatedStatuses") List<String> participatedStatuses);
}
