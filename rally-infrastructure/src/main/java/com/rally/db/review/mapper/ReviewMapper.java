package com.rally.db.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rally.db.review.entity.ReviewPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<ReviewPO> {
}
