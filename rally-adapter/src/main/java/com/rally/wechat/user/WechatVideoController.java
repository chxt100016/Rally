package com.rally.wechat.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.VideoCallbackCmd;
import com.rally.domain.user.model.VideoTokenVO;
import com.rally.user.ProfileAppService;
import com.rally.web.auth.UserContext;
import com.rally.web.user.VideoController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat/user/video")
public class WechatVideoController extends VideoController {


}
