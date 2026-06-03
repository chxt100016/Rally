package com.rally.wechat.user;

import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tennis.model.Result;
import com.rally.domain.user.model.EditProfileCmd;
import com.rally.domain.user.model.NtrpUpdateCmd;
import com.rally.domain.user.model.PlayerHomeVO;
import com.rally.domain.user.model.TennisProfileVO;
import com.rally.user.ProfileAppService;
import com.rally.web.auth.UserContext;
import com.rally.web.user.ProfileController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat/user/profile")
public class WechatProfileController extends ProfileController {

}
