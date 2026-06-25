package com.rally.web.recap;

import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.SkipReviewCmd;
import com.rally.domain.tour.model.Result;
import com.rally.recap.ReviewAppService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recap/review")
public class ReviewController {

    @Resource
    private ReviewAppService reviewAppService;

    @PostMapping("/skip")
    public Result<?> skipReview(@Valid @RequestBody SkipReviewCmd cmd) {
        reviewAppService.skipReview(cmd);
        return Result.ok();
    }

    @PostMapping
    public Result<?> submitReview(@Valid @RequestBody ReviewSubmitCmd cmd) {
        reviewAppService.submitReview(cmd);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<?> myReview() {
        return Result.ok(reviewAppService.queryMyReview());
    }
}
