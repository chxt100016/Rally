package com.rally.web.meetup;

import com.rally.domain.meetup.model.ChatMessageDTO;
import com.rally.domain.meetup.model.ChatPullCmd;
import com.rally.domain.meetup.model.ChatPullDTO;
import com.rally.domain.meetup.model.ChatSendCmd;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.ChatAppService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 活动群聊Controller
 */
@RestController
@RequestMapping("/meetup/chat")
public class ChatController {

    @Resource
    private ChatAppService chatAppService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<ChatMessageDTO> send(@Valid @RequestBody ChatSendCmd cmd) {
        return Result.ok(chatAppService.send(cmd));
    }

    /**
     * 拉取消息
     */
    @GetMapping("/pull")
    public Result<ChatPullDTO> pull(@RequestParam("meetupId") String meetupId, @RequestParam(value = "lastMessageId", required = false) String lastMessageId, @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.ok(chatAppService.pull(meetupId, lastMessageId, limit));
    }

}
