package com.rally.web.meetup;

import com.rally.domain.meetup.model.ChatPullCmd;
import com.rally.domain.meetup.model.ChatPullDTO;
import com.rally.domain.meetup.model.ChatSendCmd;
import com.rally.domain.meetup.model.ChatSendDTO;
import com.rally.domain.tennis.model.Result;
import com.rally.meetup.ChatAppService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
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
    public Result<ChatSendDTO> sendMessage(@RequestBody ChatSendCmd cmd) {
        String userId = UserContext.get();
        return Result.ok(chatAppService.sendMessage(userId, cmd));
    }

    /**
     * 拉取消息
     */
    @GetMapping("/pull")
    public Result<ChatPullDTO> pullMessages(@RequestParam("meetupId") String meetupId, @RequestParam(value = "lastMessageId", required = false) String lastMessageId, @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        String userId = UserContext.get();
        ChatPullCmd cmd = new ChatPullCmd();
        cmd.setMeetupId(meetupId);
        cmd.setLastMessageId(lastMessageId);
        cmd.setLimit(limit);
        return Result.ok(chatAppService.pullMessages(userId, cmd));
    }

}
