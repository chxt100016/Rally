package com.rally.wechat.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.OnboardingCmd;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.user.OnboardingAppService;
import com.rally.web.auth.UserContext;
import com.rally.web.user.OnboardingController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat/user/onboarding")
public class WechatOnboardingController extends OnboardingController {


}
