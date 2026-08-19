package com.rally.domain.meetup.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.meetup.gateway.ChatMessageRepository;
import com.rally.domain.meetup.gateway.ChatUserRepository;
import com.rally.domain.meetup.enums.ChatContentTypeEnum;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatUnreadUserData;
import com.rally.domain.meetup.model.ChatUserData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import com.rally.domain.auth.enums.BizErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 聊天领域服务
 * <p>消息ID体系统一使用 bizId（雪花ID，单调递增）：拉取游标、已读位置均为 bizId
 */
@Service
@RequiredArgsConstructor
public class ChatDomainService {

    /** 无游标拉取时最多回溯的历史消息条数 */
    private static final int MAX_HISTORY_COUNT = 200;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatUserRepository chatUserRepository;

    /**
     * 加入聊天
     */
    public void join(String refId, String userId) {
        boolean exists = chatUserRepository.existsByRefIdAndUserId(refId, userId);
        Assert.isTrue(!exists, BizErrorCode.ALREADY_JOINED_CHAT);

        ChatUserData chatUser = new ChatUserData();
        chatUser.setBizId(IdWorker.getIdStr());
        chatUser.setRefId(refId);
        chatUser.setUserId(userId);
        chatUser.setUnreadCount(0);
        chatUser.setJoinedAt(LocalDateTime.now());
        chatUserRepository.save(chatUser);
    }

    /**
     * 退出聊天
     */
    public void quit(String refId, String userId) {
        chatUserRepository.deleteByRefIdAndUserId(refId, userId);
    }

    /**
     * 发送消息
     */
    public ChatMessageData send(String refId, String content, ChatContentTypeEnum contentType, UserProfile sender) {

        // 创建消息
        ChatMessageData message = new ChatMessageData();
        message.setBizId(IdWorker.getIdStr());
        message.setRefId(refId);
        message.setSenderId(sender.getUserId());
        message.setSenderName(sender.getUser().getNickname());
        message.setSenderAvatar(sender.getUser().getAvatarUrl());
        message.setContent(content);
        message.setContentType(contentType);
        message.setCreateTime(LocalDateTime.now());

        // 保存消息
        chatMessageRepository.save(message);

        // 增加其他用户的未读数
        chatUserRepository.incrementUnreadCountForAllExceptSender(refId, sender.getUserId());

        // 发送者视为已读到自己发的这条消息
        advanceReadPosition(refId, sender.getUserId(), message.getBizId());
        return message;
    }

    /**
     * 拉取消息
     */
    public List<ChatMessageData> pull(String refId, String userId, String lastMessageId, Integer limit) {
        // 无游标（首拉/清缓存）时历史回溯上限200条：超过则把游标定位到最近200条之前，从那里开始拉
        if (StringUtils.isBlank(lastMessageId)) {
            lastMessageId = chatMessageRepository.findCursorBeforeRecent(refId, MAX_HISTORY_COUNT);
        }

        List<ChatMessageData> messages = chatMessageRepository.findByRefId(refId, lastMessageId, limit);

        // 维护数据库已读位置和未读数（仅用于未读数计算，不参与拉取游标）
        markAsRead(refId, userId, messages);

        return messages;
    }

    /**
     * 评论式查询：按时间倒序返回最新内容，beforeMessageId 用于继续向前翻历史。
     * 首次查询会把已读位置推进到当前最新消息；翻阅历史不会让已读位置回退。
     */
    public List<ChatMessageData> listLatest(String refId, String userId, String beforeMessageId, Integer limit) {
        List<ChatMessageData> messages = chatMessageRepository.findLatestByRefId(refId, beforeMessageId, limit);
        if (!messages.isEmpty()) {
            advanceReadPosition(refId, userId, messages.get(0).getBizId());
        }
        return messages;
    }



    /**
     * 维护数据库的已读状态：已读位置只前进不后退，并按新位置重算剩余未读数。
     */
    private void markAsRead(String refId, String userId, List<ChatMessageData> messages) {
        // 没拉到新消息，无需变更已读状态
        if (messages.isEmpty()) {
            return;
        }
        String latestMessageId = messages.get(messages.size() - 1).getBizId();
        advanceReadPosition(refId, userId, latestMessageId);
    }

    /**
     * 将用户的已读位置推进到 messageId（只前进不后退）。
     */
    private void advanceReadPosition(String refId, String userId, String messageId) {
        ChatUserData chatUser = chatUserRepository.findByRefIdAndUserId(refId, userId);
        if (chatUser == null) {
            // 首次产生已读记录，创建聊天用户记录
            chatUser = new ChatUserData();
            chatUser.setBizId(IdWorker.getIdStr());
            chatUser.setRefId(refId);
            chatUser.setUserId(userId);
            chatUser.setLastReadMessageId(messageId);
            chatUser.setLastReadTime(LocalDateTime.now());
            chatUser.setUnreadCount(chatMessageRepository.countByRefIdAfterMessageId(refId, messageId));
            chatUser.setJoinedAt(LocalDateTime.now());
            chatUserRepository.save(chatUser);
            return;
        }

        // 已读位置只前进不后退（雪花ID等长，字符串比较即数值比较）
        if (isAfter(messageId, chatUser.getLastReadMessageId())) {
            chatUserRepository.updateLastReadMessageId(refId, userId, messageId, LocalDateTime.now());
            Integer remainingUnreadCount = chatMessageRepository.countByRefIdAfterMessageId(refId, messageId);
            chatUserRepository.updateUnreadCount(refId, userId, remainingUnreadCount);
        }
    }

    /**
     * 判断 messageId 是否在 baseline 之后（baseline 为空视为最早）
     */
    private boolean isAfter(String messageId, String baseline) {
        if (baseline == null || baseline.isEmpty()) {
            return true;
        }
        return messageId.compareTo(baseline) > 0;
    }

    /**
     * 获取未读最新消息的用户列表（以活动参与者列表为基准，含从未打开过聊天/已退出聊天的人）
     * @param participantIds 活动全部参与者userId
     */
    public List<ChatUnreadUserData> getUnreadUsers(String refId, List<String> participantIds) {
        String latestMessageId = chatMessageRepository.findLatestMessageId(refId);
        // 无消息时人人都算已读
        if (latestMessageId == null) {
            return List.of();
        }

        Map<String, ChatUserData> chatUserMap = chatUserRepository.findMapByRefId(refId);

        List<ChatUnreadUserData> unreadUsers = new ArrayList<>();
        for (String userId : participantIds) {
            ChatUserData chatUser = chatUserMap.get(userId);
            // 未加入过聊天/已退出聊天，视为未读
            if (chatUser == null) {
                unreadUsers.add(new ChatUnreadUserData(userId, null));
                continue;
            }
            if (isAfter(latestMessageId, chatUser.getLastReadMessageId())) {
                unreadUsers.add(new ChatUnreadUserData(userId, chatUser.getLastReadTime()));
            }
        }
        return unreadUsers;
    }

    /**
     * 获取未读数
     */
    public Integer getUnreadCount(String refId, String userId) {

        // 查询用户的聊天记录
        ChatUserData chatUser = chatUserRepository.findByRefIdAndUserId(refId, userId);

        // 如果没有记录，直接count所有消息作为未读数
        if (chatUser == null) {
            return chatMessageRepository.countByRefIdAfterMessageId(refId, null);
        }

        return chatUser.getUnreadCount();
    }

}
