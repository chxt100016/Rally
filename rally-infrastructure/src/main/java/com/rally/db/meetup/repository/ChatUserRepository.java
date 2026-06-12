package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.db.meetup.convert.ChatConvertMapper;
import com.rally.db.meetup.entity.ChatUserPO;
import com.rally.db.meetup.service.ChatUserService;
import com.rally.domain.meetup.model.ChatUserData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天用户Repository
 */
@Component
@RequiredArgsConstructor
public class ChatUserRepository implements com.rally.domain.meetup.gateway.ChatUserRepository {

    private final ChatUserService chatUserService;

    @Override
    public void save(ChatUserData data) {
        ChatUserPO po = ChatConvertMapper.INSTANCE.toChatUserPO(data);
        chatUserService.save(po);
    }

    @Override
    public ChatUserData findByMeetupIdAndUserId(String meetupId, String userId) {
        LambdaQueryWrapper<ChatUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatUserPO::getMeetupId, meetupId)
                .eq(ChatUserPO::getUserId, userId);
        ChatUserPO po = chatUserService.getOne(wrapper);
        return po != null ? ChatConvertMapper.INSTANCE.toChatUserData(po) : null;
    }

    @Override
    public void updateLastReadMessageId(String meetupId, String userId, String lastReadMessageId) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getMeetupId, meetupId)
                .eq(ChatUserPO::getUserId, userId)
                .set(ChatUserPO::getLastReadMessageId, lastReadMessageId);
        chatUserService.update(wrapper);
    }

    @Override
    public void updateUnreadCount(String meetupId, String userId, Integer unreadCount) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getMeetupId, meetupId)
                .eq(ChatUserPO::getUserId, userId)
                .set(ChatUserPO::getUnreadCount, unreadCount);
        chatUserService.update(wrapper);
    }

    @Override
    public void incrementUnreadCountForAllExceptSender(String meetupId, String senderId) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getMeetupId, meetupId)
                .ne(ChatUserPO::getUserId, senderId)
                .setSql("unread_count = unread_count + 1");
        chatUserService.update(wrapper);
    }
}
