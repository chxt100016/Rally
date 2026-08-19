package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.meetup.convert.ChatConvertMapper;
import com.rally.db.meetup.entity.ChatMessagePO;
import com.rally.db.meetup.service.ChatMessageService;
import com.rally.domain.meetup.model.ChatMessageData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天消息Repository
 */
@Component
@RequiredArgsConstructor
public class ChatMessageRepository implements com.rally.domain.meetup.gateway.ChatMessageRepository {

    private final ChatMessageService chatMessageService;

    @Override
    public void save(ChatMessageData data) {
        ChatMessagePO po = ChatConvertMapper.INSTANCE.toChatMessagePO(data);
        chatMessageService.save(po);
    }

    @Override
    public List<ChatMessageData> findByRefId(String refId, String lastMessageId, Integer limit) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<ChatMessagePO>()
                .eq(ChatMessagePO::getRefId, refId)
                .gt(StringUtils.isNotBlank(lastMessageId), ChatMessagePO::getBizId, lastMessageId)
                .orderByAsc(ChatMessagePO::getBizId)
                .last(limit != null && limit > 0, "LIMIT " + limit);

        List<ChatMessagePO> poList = chatMessageService.list(wrapper);
        return poList.stream()
                .map(ChatConvertMapper.INSTANCE::toChatMessageData)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageData> findLatestByRefId(String refId, String beforeMessageId, Integer limit) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<ChatMessagePO>()
                .eq(ChatMessagePO::getRefId, refId)
                .lt(StringUtils.isNotBlank(beforeMessageId), ChatMessagePO::getBizId, beforeMessageId)
                .orderByDesc(ChatMessagePO::getBizId)
                .last(limit != null && limit > 0, "LIMIT " + limit);

        return chatMessageService.list(wrapper).stream()
                .map(ChatConvertMapper.INSTANCE::toChatMessageData)
                .collect(Collectors.toList());
    }

    @Override
    public String findCursorBeforeRecent(String refId, int recentCount) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        // 按bizId倒序跳过最近 recentCount 条，取到的即历史回溯的起始游标
        wrapper.select(ChatMessagePO::getBizId)
                .eq(ChatMessagePO::getRefId, refId)
                .orderByDesc(ChatMessagePO::getBizId)
                .last("LIMIT " + recentCount + ", 1");
        ChatMessagePO po = chatMessageService.getOne(wrapper);
        return po != null ? po.getBizId() : null;
    }

    @Override
    public Integer countByRefIdAfterMessageId(String refId, String afterMessageId) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePO::getRefId, refId)
                .gt(StringUtils.isNotBlank(afterMessageId), ChatMessagePO::getBizId, afterMessageId);
        return Math.toIntExact(chatMessageService.count(wrapper));
    }

    @Override
    public String findLatestMessageId(String refId) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<ChatMessagePO>()
                .select(ChatMessagePO::getBizId)
                .eq(ChatMessagePO::getRefId, refId)
                .orderByDesc(ChatMessagePO::getBizId)
                .last("LIMIT 1");
        ChatMessagePO po = chatMessageService.getOne(wrapper);
        return po != null ? po.getBizId() : null;
    }
}
