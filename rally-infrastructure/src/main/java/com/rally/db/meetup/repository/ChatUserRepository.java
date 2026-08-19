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
    public ChatUserData findByRefIdAndUserId(String refId, String userId) {
        LambdaQueryWrapper<ChatUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .eq(ChatUserPO::getUserId, userId);
        ChatUserPO po = chatUserService.getOne(wrapper);
        return po != null ? ChatConvertMapper.INSTANCE.toChatUserData(po) : null;
    }

    @Override
    public boolean existsByRefIdAndUserId(String refId, String userId) {
        LambdaQueryWrapper<ChatUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .eq(ChatUserPO::getUserId, userId);
        return chatUserService.count(wrapper) > 0;
    }

    @Override
    public void updateLastReadMessageId(String refId, String userId, String lastReadMessageId, java.time.LocalDateTime lastReadTime) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .eq(ChatUserPO::getUserId, userId)
                .set(ChatUserPO::getLastReadMessageId, lastReadMessageId)
                .set(ChatUserPO::getLastReadTime, lastReadTime);
        chatUserService.update(wrapper);
    }

    @Override
    public java.util.Map<String, ChatUserData> findMapByRefId(String refId) {
        LambdaQueryWrapper<ChatUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId);
        List<ChatUserPO> poList = chatUserService.list(wrapper);
        return poList.stream()
                .map(ChatConvertMapper.INSTANCE::toChatUserData)
                .collect(Collectors.toMap(ChatUserData::getUserId, d -> d, (a, b) -> a));
    }

    @Override
    public void updateUnreadCount(String refId, String userId, Integer unreadCount) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .eq(ChatUserPO::getUserId, userId)
                .set(ChatUserPO::getUnreadCount, unreadCount);
        chatUserService.update(wrapper);
    }

    @Override
    public void incrementUnreadCountForAllExceptSender(String refId, String senderId) {
        LambdaUpdateWrapper<ChatUserPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .ne(ChatUserPO::getUserId, senderId)
                .setSql("unread_count = unread_count + 1");
        chatUserService.update(wrapper);
    }

    @Override
    public void deleteByRefIdAndUserId(String refId, String userId) {
        LambdaQueryWrapper<ChatUserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatUserPO::getRefId, refId)
                .eq(ChatUserPO::getUserId, userId);
        chatUserService.remove(wrapper);
    }
}
