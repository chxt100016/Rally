package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.ChatMessageData;

import java.util.List;

/**
 * 聊天消息网关接口
 */
public interface ChatMessageRepository {

    /**
     * 保存消息
     */
    void save(ChatMessageData data);

    /**
     * 查询关联业务的消息列表（分页）
     * @param refId 关联业务ID
     * @param lastMessageId 上次拉取的最新消息bizId，为空则从头拉取历史消息
     * @param limit 条数限制
     * @return 消息列表
     */
    List<ChatMessageData> findByRefId(String refId, String lastMessageId, Integer limit);

    /**
     * 按消息ID倒序查询最新消息，用于评论式历史分页。
     * @param beforeMessageId 查询此消息之前的内容，为空时从最新消息开始
     */
    List<ChatMessageData> findLatestByRefId(String refId, String beforeMessageId, Integer limit);

    /**
     * 查询某条消息之后的消息数（用于未读数计算）
     * @param afterMessageId 消息bizId，为空则统计全部
     */
    Integer countByRefIdAfterMessageId(String refId, String afterMessageId);

    /**
     * 查询最近 recentCount 条消息之前的那条消息的bizId（用作历史回溯的起始游标）
     * @return 第 recentCount+1 新的消息bizId；总数不超过 recentCount 时返回 null
     */
    String findCursorBeforeRecent(String refId, int recentCount);

    /**
     * 查询关联业务最新一条消息的bizId
     * @return 无消息返回 null
     */
    String findLatestMessageId(String refId);
}
