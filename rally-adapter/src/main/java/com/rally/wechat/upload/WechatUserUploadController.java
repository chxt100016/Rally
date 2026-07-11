package com.rally.wechat.upload;

import com.rally.web.upload.UserUploadController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/user/upload")
public class WechatUserUploadController extends UserUploadController {
}
