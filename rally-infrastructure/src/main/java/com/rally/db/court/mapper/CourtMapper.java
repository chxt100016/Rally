package com.rally.db.court.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.court.entity.CourtPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CourtMapper extends BaseMapper<CourtPO> {

    /**
     * 增加球场约球次数
     * @param courtId 球场 bizId
     * @param count 增加次数
     */
    @Update("UPDATE rally_court SET meetup_count = COALESCE(meetup_count, 0) + #{count} WHERE biz_id = #{courtId}")
    void incrementMeetupCount(@Param("courtId") String courtId, @Param("count") Integer count);
}
