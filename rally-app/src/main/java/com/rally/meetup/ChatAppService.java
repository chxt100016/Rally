package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupDomainService;
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

    private final MeetupDomainService meetupDomainService;

    /**
     * 发送消息
     */
    public ChatMessageDTO send(ChatSendCmd cmd) {
        String senderId = UserContext.get();
        assertIn(cmd.getMeetupId(), senderId);

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
        assertIn(meetupId, userId);

        // 拉取消息
        List<ChatMessageData> messages = chatDomainService.pull(meetupId, userId, lastMessageId, limit);

        // 转换消息列表
        List<ChatMessageDTO> messageDTOs = ChatAppConvertMapper.INSTANCE.toChatMessageDTO(messages);

        return new ChatPullDTO(messageDTOs);
    }

    /**
     * 断言用户是活动参与者（创建者或已报名用户）
     * @param meetupId 活动ID
     * @param userId 用户ID
     */
    public void assertIn(String meetupId, String userId) {
        Meetup meetup = meetupDomainService.get(meetupId);
        meetup.assertIn(userId);
    }
}
