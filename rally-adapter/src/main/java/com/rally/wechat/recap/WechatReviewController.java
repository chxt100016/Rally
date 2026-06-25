package com.rally.wechat.recap;

import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.SkipReviewCmd;
import com.rally.domain.tour.model.Result;
import com.rally.recap.ReviewAppService;
import com.rally.web.recap.ReviewController;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat/recap/review")
public class WechatReviewController extends ReviewController {


}
