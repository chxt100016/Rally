package com.rally.meetup;

import com.rally.domain.meetup.model.ChatMessageDTO;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatPullDTO;
import com.rally.domain.meetup.model.ChatSendCmd;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupPolicy;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.meetup.convert.ChatAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天应用服务
 */
@Service
@RequiredArgsConstructor
public class ChatAppService {

    private final ChatDomainService chatDomainService;

    private final UserProfileDomainService userProfileDomainService;

    private final MeetupPolicy meetupPolicy;

    /**
     * 发送消息
     */
    public ChatMessageDTO send(ChatSendCmd cmd) {
        String senderId = UserContext.get();
        meetupPolicy.assertIn(cmd.getMeetupId(), senderId);

         // 冗余发送者昵称和头像，拉取消息时无需再关联用户表
        UserProfile sender = userProfileDomainService.get(senderId);

        // 发送消息
        ChatMessageData message = chatDomainService.send(cmd, sender);

        return ChatAppConvertMapper.INSTANCE.toChatMessageDTO(message);
    }

    /**
     * 拉取消息
     */
    public ChatPullDTO pull(String meetupId, String lastMessageId, Integer limit) {
        String userId = UserContext.get();
        meetupPolicy.assertIn(meetupId, userId);

        // 无游标（首拉/清缓存）时历史回溯上限
        String fixedLastMessageId = chatDomainService.fixLastMessageId(lastMessageId, meetupId);

        // 拉取消息
        List<ChatMessageData> messages = chatDomainService.pull(meetupId, userId, fixedLastMessageId, limit);

        // 转换消息列表
        List<ChatMessageDTO> messageDTOs = ChatAppConvertMapper.INSTANCE.toChatMessageDTO(messages);

        return new ChatPullDTO(messageDTOs);
    }
}
