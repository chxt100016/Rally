package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.meetup.convert.ChatAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天应用服务
 */
@Service
@RequiredArgsConstructor
public class ChatAppService {

    private final ChatDomainService chatDomainService;

    /**
     * 发送消息
     */
    public ChatSendDTO sendMessage(String userId, ChatSendCmd cmd) {
        ChatMessageData message = chatDomainService.sendMessage(
                cmd.getMeetupId(), userId, cmd.getContent(), cmd.getContentType());

        ChatSendDTO dto = new ChatSendDTO();
        dto.setMessageId(message.getBizId());
        dto.setCreateTime(message.getCreateTime());
        return dto;
    }

    /**
     * 拉取消息
     */
    public ChatPullDTO pullMessages(String userId, ChatPullCmd cmd) {
        ChatDomainService.ChatPullData pullData = chatDomainService.pullMessages(
                cmd.getMeetupId(), userId, cmd.getLastMessageId(), cmd.getLimit());

        // 转换消息列表
        List<ChatMessageDTO> messageDTOs = pullData.getMessages().stream()
                .map(ChatAppConvertMapper.INSTANCE::toChatMessageDTO)
                .collect(Collectors.toList());

        ChatPullDTO dto = new ChatPullDTO();
        dto.setMessages(messageDTOs);
        dto.setLastMessageId(pullData.getLastMessageId());
        return dto;
    }


}
