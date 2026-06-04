package com.rally.db.review.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

@Service
public class ReviewService extends ServiceImpl<ReviewMapper, ReviewPO> {
}
