package com.rally.domain.recap;

import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.recap.gateway.ReviewRepository;
import com.rally.domain.recap.model.ReviewData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户评价领域服务
 * 封装评价聚合查询能力
 */
@Service
public class UserReviewDomainService {

    @Resource
    private ReviewRepository reviewRepository;



    /**
     * 一次查库聚合评价总数 + top N 标签（避免多次 DB 查询）
     * @param toUserId 被评价人
     * @param tagLimit 标签数量上限
     * @return ReviewSummaryDTO（total + tags）
     */
    public ReviewSummaryDTO getReviewSummary(String toUserId, int tagLimit) {
        // 一次查询获取该用户收到的全部评价
        List<ReviewData> allReviews = reviewRepository.listAllByToUser(toUserId);

        // 统计 LEVEL_VOTE 类型数量
        long levelVoteCount = allReviews.stream()
                .filter(r -> ReviewTypeEnum.LEVEL_VOTE == r.getReviewType())
                .count();

        // 统计 ATTENDANCE_VOTE 类型数量
        long attendanceVoteCount = allReviews.stream()
                .filter(r -> ReviewTypeEnum.ATTENDANCE_VOTE == r.getReviewType())
                .count();

        // TAG 类型的 value 是逗号分隔的多标签，需要 split 后分别计数
        List<TagItem> topTags = allReviews.stream()
                .filter(r -> ReviewTypeEnum.TAG == r.getReviewType())
                .flatMap(r -> Arrays.stream(r.getReviewValue().split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(tagLimit)
                .map(e -> new TagItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 标签总数
        long tagCount = allReviews.stream()
                .filter(r -> ReviewTypeEnum.TAG == r.getReviewType())
                .flatMap(r -> Arrays.stream(r.getReviewValue().split(",")))
                .filter(s -> !s.isEmpty())
                .count();

        return new ReviewSummaryDTO(levelVoteCount + attendanceVoteCount + tagCount, levelVoteCount, attendanceVoteCount, tagCount, topTags);
    }

    /**
     * 评价聚合结果（评价总数 + top 标签）
     */
    public record ReviewSummaryDTO(Long total, Long levelVoteCount, Long attendanceVoteCount, Long tagCount, List<TagItem> topTags) {}

    /**
     * 标签及其数量
     */
    public record TagItem(String name, long count) {}
}
