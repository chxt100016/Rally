package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.ChatUserData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天用户网关接口
 */
public interface ChatUserRepository {

    /**
     * 保存聊天用户
     */
    void save(ChatUserData data);

    /**
     * 查询用户在关联业务的聊天信息
     */
    ChatUserData findByRefIdAndUserId(String refId, String userId);

    /**
     * 判断用户是否已加入聊天
     */
    boolean existsByRefIdAndUserId(String refId, String userId);



    /**
     * 更新用户的已读位置和已读时间
     */
    void updateLastReadMessageId(String refId, String userId, String lastReadMessageId, LocalDateTime lastReadTime);

    /**
     * 批量查询关联业务下的聊天用户记录（key = userId）
     */
    Map<String, ChatUserData> findMapByRefId(String refId);

    /**
     * 更新用户的未读数
     */
    void updateUnreadCount(String refId, String userId, Integer unreadCount);

    /**
     * 增加关联业务所有用户的未读数（发消息时调用，排除发送者）
     */
    void incrementUnreadCountForAllExceptSender(String refId, String senderId);

    /**
     * 删除聊天用户
     */
    void deleteByRefIdAndUserId(String refId, String userId);
}
