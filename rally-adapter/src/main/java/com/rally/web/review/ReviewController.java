package com.rally.web.review;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.review.model.ReviewCmd;
import com.rally.domain.review.model.ReviewVO;
import com.rally.domain.review.model.ReviewableListVO;
import com.rally.domain.tennis.model.Result;
import com.rally.review.ReviewAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 评价接口：提交评价、可评价人列表、收到评价
 */
@RestController
@RequestMapping("/wechat/review")
public class ReviewController {

    @Resource
    private ReviewAppService reviewAppService;

    /**
     * 提交评价
     * POST /api/rally/wechat/review/submit
     */
    @PostMapping("/submit")
    public Result<Void> submitReview(@Valid @RequestBody ReviewCmd cmd) {
        try {
            reviewAppService.submitReview(cmd);
            return Result.ok();
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 获取某场我可评价的人列表与已评状态
     * GET /api/rally/wechat/review/reviewable?rallyMeetupId=xxx
     */
    @GetMapping("/reviewable")
    public Result<ReviewableListVO> reviewable(@RequestParam("rallyMeetupId") String rallyMeetupId) {
        try {
            return Result.ok(reviewAppService.getReviewableList(rallyMeetupId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }

    /**
     * 获取某人收到的评价（球员主页三区分组）
     * GET /api/rally/wechat/review/received?toUserId=xxx
     */
    @GetMapping("/received")
    public Result<ReviewVO> received(@RequestParam("toUserId") String toUserId) {
        try {
            return Result.ok(reviewAppService.getReceivedReviews(toUserId));
        } catch (BusinessException e) {
            return Result.fail(e.getErrorCode().getCode(), e.getMessage());
        }
    }
}
