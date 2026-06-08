package com.rally.domain.recap;

import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.recap.gateway.ReviewGateway;
import com.rally.domain.recap.model.ReviewData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户评价领域服务
 * 封装评价聚合查询能力
 */
@Service
public class UserReviewDomainService {

    @Resource
    private ReviewGateway reviewGateway;



    /**
     * 一次查库聚合评价总数 + top N 标签（避免多次 DB 查询）
     * @param toUserId 被评价人
     * @param tagLimit 标签数量上限
     * @return ReviewSummaryDTO（total + tags）
     */
    public ReviewSummaryDTO getReviewSummary(String toUserId, int tagLimit) {
        // 一次查询获取该用户收到的全部评价
        List<ReviewData> allReviews = reviewGateway.listAllByToUser(toUserId);
        // 评价总数
        int total = allReviews.size();
        // 从 TAG 类型评价中提取 top N 标签
        List<String> topTags = allReviews.stream()
                .filter(r -> ReviewTypeEnum.TAG.name().equals(r.getReviewType().name()))
                .collect(Collectors.groupingBy(ReviewData::getReviewValue, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(tagLimit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return new ReviewSummaryDTO(total, topTags);
    }

    /**
     * 评价聚合结果（评价总数 + top 标签）
     */
    public record ReviewSummaryDTO(int total, List<String> topTags) {}
}
