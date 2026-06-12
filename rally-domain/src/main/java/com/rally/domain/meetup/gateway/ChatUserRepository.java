package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.ChatUserData;

import java.util.List;

/**
 * 聊天用户网关接口
 */
public interface ChatUserRepository {

    /**
     * 保存聊天用户
     */
    void save(ChatUserData data);

    /**
     * 查询用户在活动的聊天信息
     */
    ChatUserData findByMeetupIdAndUserId(String meetupId, String userId);

    /**
     * 查询活动的所有聊天用户
     */
    List<ChatUserData> findByMeetupId(String meetupId);

    /**
     * 更新用户的已读位置
     */
    void updateLastReadMessageId(String meetupId, String userId, String lastReadMessageId);

    /**
     * 更新用户的未读数
     */
    void updateUnreadCount(String meetupId, String userId, Integer unreadCount);

    /**
     * 增加活动所有用户的未读数（发消息时调用，排除发送者）
     */
    void incrementUnreadCountForAllExceptSender(String meetupId, String senderId);
}
