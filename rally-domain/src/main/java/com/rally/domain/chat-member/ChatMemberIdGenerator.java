package com.rally.domain.meetup.chatmember;

/** 首次加入或修复缺失关系时生成聊天成员业务编号。 */
@FunctionalInterface
public interface ChatMemberIdGenerator {

    String nextMemberId();
}
