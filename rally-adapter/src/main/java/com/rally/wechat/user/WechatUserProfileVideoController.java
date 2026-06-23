package com.rally.wechat.user;

import com.rally.domain.tour.model.Result;
import com.rally.domain.user.model.DeleteVideoCmd;
import com.rally.domain.user.model.MyProfileDTO;
import com.rally.domain.user.model.UpdateVideoCmd;
import com.rally.domain.user.model.UploadVideoCmd;
import com.rally.user.ProfileAppService;
import com.rally.web.user.UserProfileVideoController;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户视频管理
 */
@RestController
@RequestMapping("/wechat/user/profile/video")
public class WechatUserProfileVideoController extends UserProfileVideoController {

}
