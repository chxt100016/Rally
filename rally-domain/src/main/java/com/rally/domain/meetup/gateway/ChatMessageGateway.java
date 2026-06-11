package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.ChatMessageData;

import java.util.List;

/**
 * 聊天消息网关接口
 */
public interface ChatMessageGateway {

    /**
     * 保存消息
     */
    void save(ChatMessageData data);

    /**
     * 查询活动的消息列表（分页）
     * @param meetupId 活动ID
     * @param lastMessageId 上次拉取的最新消息bizId，为空则从头拉取历史消息
     * @param limit 条数限制
     * @return 消息列表
     */
    List<ChatMessageData> findByMeetupId(String meetupId, String lastMessageId, Integer limit);

    /**
     * 查询某条消息之后的消息数（用于未读数计算）
     * @param afterMessageId 消息bizId，为空则统计全部
     */
    Integer countByMeetupIdAfterMessageId(String meetupId, String afterMessageId);

    /**
     * 查询最近 recentCount 条消息之前的那条消息的bizId（用作历史回溯的起始游标）
     * @return 第 recentCount+1 新的消息bizId；总数不超过 recentCount 时返回 null
     */
    String findCursorBeforeRecent(String meetupId, int recentCount);
}
