package com.rally.wechat.meetup;

import com.rally.web.meetup.ChatController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/wechat/meetup/chat")
@RequiredArgsConstructor
public class WechatChatController extends ChatController {
}
