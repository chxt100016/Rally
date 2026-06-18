package com.rally.domain.meetup.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.meetup.gateway.ChatMessageRepository;
import com.rally.domain.meetup.gateway.ChatUserRepository;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.model.ChatSendCmd;
import com.rally.domain.meetup.model.ChatUserData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import com.rally.domain.auth.enums.BizErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
    public void join(String meetupId, String userId) {
        boolean exists = chatUserRepository.existsByMeetupIdAndUserId(meetupId, userId);
        Assert.isTrue(!exists, BizErrorCode.ALREADY_JOINED_CHAT);

        ChatUserData chatUser = new ChatUserData();
        chatUser.setBizId(IdWorker.getIdStr());
        chatUser.setMeetupId(meetupId);
        chatUser.setUserId(userId);
        chatUser.setUnreadCount(0);
        chatUser.setJoinedAt(LocalDateTime.now());
        chatUserRepository.save(chatUser);
    }

    /**
     * 退出聊天
     */
    public void quit(String meetupId, String userId) {
        chatUserRepository.deleteByMeetupIdAndUserId(meetupId, userId);
    }

    /**
     * 发送消息
     */
    public ChatMessageData send(ChatSendCmd cmd, UserProfile sender) {

        // 创建消息
        ChatMessageData message = new ChatMessageData();
        message.setBizId(IdWorker.getIdStr());
        message.setMeetupId(cmd.getMeetupId());
        message.setSenderId(sender.getUserId());
        message.setSenderName(sender.getUser().getNickname());
        message.setSenderAvatar(sender.getUser().getAvatarUrl());
        message.setContent(cmd.getContent());
        message.setContentType(cmd.getContentType());
        message.setCreateTime(LocalDateTime.now());

        // 保存消息
        chatMessageRepository.save(message);

        // 增加其他用户的未读数
        chatUserRepository.incrementUnreadCountForAllExceptSender(cmd.getMeetupId(), sender.getUserId());
        return message;
    }

    /**
     * 拉取消息
     */
    public List<ChatMessageData> pull(String meetupId, String userId, String lastMessageId, Integer limit) {
        // 无游标（首拉/清缓存）时历史回溯上限200条：超过则把游标定位到最近200条之前，从那里开始拉
        if (StringUtils.isBlank(lastMessageId)) {
            lastMessageId = chatMessageRepository.findCursorBeforeRecent(meetupId, MAX_HISTORY_COUNT);
        }

        List<ChatMessageData> messages = chatMessageRepository.findByMeetupId(meetupId, lastMessageId, limit);

        // 维护数据库已读位置和未读数（仅用于未读数计算，不参与拉取游标）
        markAsRead(meetupId, userId, messages);

        return messages;
    }



    /**
     * 维护数据库的已读状态：清零未读数，已读位置只前进不后退（防止清缓存重拉历史时把已读位置回退）
     */
    private void markAsRead(String meetupId, String userId, List<ChatMessageData> messages) {
        // 没拉到新消息，无需变更已读状态
        if (messages.isEmpty()) {
            return;
        }

        String latestMessageId = messages.get(messages.size() - 1).getBizId();
        ChatUserData chatUser = chatUserRepository.findByMeetupIdAndUserId(meetupId, userId);
        if (chatUser == null) {
            // 首次拉取，创建聊天用户记录
            chatUser = new ChatUserData();
            chatUser.setBizId(IdWorker.getIdStr());
            chatUser.setMeetupId(meetupId);
            chatUser.setUserId(userId);
            chatUser.setLastReadMessageId(latestMessageId);
            chatUser.setUnreadCount(0);
            chatUser.setJoinedAt(LocalDateTime.now());
            chatUserRepository.save(chatUser);
            return;
        }

        // 已读位置只前进不后退（雪花ID等长，字符串比较即数值比较）
        if (isAfter(latestMessageId, chatUser.getLastReadMessageId())) {
            chatUserRepository.updateLastReadMessageId(meetupId, userId, latestMessageId);
        }
        chatUserRepository.updateUnreadCount(meetupId, userId, 0);
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
     * 获取未读数
     */
    public Integer getUnreadCount(String meetupId, String userId) {

        // 查询用户的聊天记录
        ChatUserData chatUser = chatUserRepository.findByMeetupIdAndUserId(meetupId, userId);

        // 如果没有记录，直接count所有消息作为未读数
        if (chatUser == null) {
            return chatMessageRepository.countByMeetupIdAfterMessageId(meetupId, null);
        }

        return chatUser.getUnreadCount();
    }

}
