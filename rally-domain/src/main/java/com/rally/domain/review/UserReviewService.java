package com.rally.domain.review;

import com.rally.domain.review.enums.ReviewTypeEnum;
import com.rally.domain.review.gateway.ReviewGateway;
import com.rally.domain.review.model.ReviewData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户评价领域服务
 * 封装评价聚合查询能力
 */
@Service
public class UserReviewService {

    @Resource
    private ReviewGateway reviewGateway;

    /**
     * 统计用户收到的评价总数（所有维度）
     */
    public int countByToUser(String toUserId) {
        int ntrpCount = reviewGateway.listByToUserAndType(toUserId, ReviewTypeEnum.NTRP_VOTE.name()).size();
        int tagCount = reviewGateway.listByToUserAndType(toUserId, ReviewTypeEnum.TAG.name()).size();
        int attendanceCount = reviewGateway.listByToUserAndType(toUserId, ReviewTypeEnum.ATTENDANCE.name()).size();
        return ntrpCount + tagCount + attendanceCount;
    }

    /**
     * 获取用户收到的 top N 标签（按出现频次降序）
     */
    public List<String> getTopTags(String toUserId, int limit) {
        List<Object[]> stats = reviewGateway.countByToUserGroupByValue(toUserId, ReviewTypeEnum.TAG.name());
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }
        return stats.stream()
                .sorted((a, b) -> ((Number) b[1]).intValue() - ((Number) a[1]).intValue())
                .limit(limit)
                .map(row -> (String) row[0])
                .collect(Collectors.toList());
    }
}
