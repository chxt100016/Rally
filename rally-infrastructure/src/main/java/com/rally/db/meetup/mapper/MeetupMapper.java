package com.rally.db.meetup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rally.db.meetup.entity.MeetupPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MeetupMapper extends BaseMapper<MeetupPO> {

    /**
     * PENDING tab 联合查询：创建人有待审批 + 参与者已结束未录比分
     * UNION 后分页
     */
    @Select("<script>" +
            "SELECT * FROM (" +
            " (SELECT m.* FROM rally_meetup m" +
            "  WHERE m.creator_id = #{userId}" +
            "  AND m.status IN ('OPEN','FULL') AND m.end_time > NOW()" +
            "  AND EXISTS (SELECT 1 FROM rally_meetup_registration r" +
            "    WHERE r.rally_meetup_id = m.biz_id AND r.status = 'pending'))" +
            " UNION" +
            " (SELECT m.* FROM rally_meetup m" +
            "  WHERE (m.status = 'FINISHED' OR (m.status IN ('OPEN','FULL') AND m.end_time &lt; NOW()))" +
            "  AND m.end_time + INTERVAL #{deadlineDays} DAY > NOW()" +
            "  AND EXISTS (SELECT 1 FROM rally_meetup_registration r" +
            "    WHERE r.rally_meetup_id = m.biz_id AND r.user_id = #{userId} AND r.status = 'JOINED')" +
            "  AND NOT EXISTS (SELECT 1 FROM rally_meetup_score s WHERE s.rally_meetup_id = m.biz_id)" +
            ") ORDER BY create_time DESC" +
            ") t" +
            "</script>")
    IPage<MeetupPO> selectPendingMeetups(IPage<MeetupPO> page,
                                          @Param("userId") String userId,
                                          @Param("deadlineDays") int deadlineDays);
}
