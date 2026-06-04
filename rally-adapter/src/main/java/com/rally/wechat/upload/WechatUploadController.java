package com.rally.wechat.upload;

import com.rally.web.upload.UploadController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/wechat/upload")
public class WechatUploadController extends UploadController {

}
