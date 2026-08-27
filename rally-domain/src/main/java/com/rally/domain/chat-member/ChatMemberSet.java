package com.rally.domain.meetup.chatmember;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一个业务频道的聊天成员集合聚合根。
 *
 * <p>聚合按 {@code refId} 装载完整当前成员集合。所有成员关系和阅读状态变更只通过
 * C1-C4 进入；仓储应在同一事务内保存 {@link #members()} 并物理删除
 * {@link #removedMembers()}。</p>
 */
public final class ChatMemberSet {

    public static final String CHAT_MEMBER_DUPLICATE = "CHAT_MEMBER_DUPLICATE";
    public static final String CHAT_READ_STATE_INVALID = "CHAT_READ_STATE_INVALID";
    public static final String CHAT_READ_POSITION_REGRESSION = "CHAT_READ_POSITION_REGRESSION";
    public static final String CHAT_UNREAD_DIVERGED = "CHAT_UNREAD_DIVERGED";
    public static final String ALREADY_JOINED_CHAT = "ALREADY_JOINED_CHAT";

    private final String refId;
    private final Map<String, ChatMemberState> membersByUser;
    private final List<ChatMemberState> removedMembers = new ArrayList<>();

    private ChatMemberSet(String refId, Collection<ChatMemberState> members) {
        requireNotBlank(refId, CHAT_READ_STATE_INVALID, "频道业务编号不能为空");
        this.refId = refId;
        this.membersByUser = new LinkedHashMap<>();
        if (members != null) {
            for (ChatMemberState member : members) {
                require(member != null, CHAT_READ_STATE_INVALID, "成员状态不能为空");
                ChatMemberState previous = this.membersByUser.putIfAbsent(member.userId(), member);
                require(previous == null, CHAT_MEMBER_DUPLICATE, "同一频道与用户存在重复成员记录");
            }
        }
        checkInvariants();
    }

    /** 从仓储按同一 refId 载入的完整当前成员集合恢复聚合。 */
    public static ChatMemberSet restore(String refId, Collection<ChatMemberState> members) {
        return new ChatMemberSet(refId, members);
    }

    /** 建立一个尚无成员的频道集合。 */
    public static ChatMemberSet empty(String refId) {
        return new ChatMemberSet(refId, List.of());
    }

    /** C1：显式加入频道；重复加入必须返回稳定业务错误。 */
    public ChatMemberState join(JoinChatCommand command, ChatMemberIdGenerator idGenerator) {
        require(command != null, CHAT_READ_STATE_INVALID, "加入频道命令不能为空");
        requireNotBlank(command.userId(), CHAT_READ_STATE_INVALID, "用户编号不能为空");
        if (membersByUser.containsKey(command.userId())) {
            throw error(ALREADY_JOINED_CHAT, "用户已是频道成员");
        }

        ChatMemberState joined = ChatMemberState.explicitlyJoined(
                nextUniqueMemberId(idGenerator), refId, command);
        membersByUser.put(joined.userId(), joined);
        checkInvariants();
        require(isBlank(joined.lastReadMessageId())
                        && joined.lastReadTime() == null
                        && joined.unreadCount() == 0,
                CHAT_READ_STATE_INVALID,
                "显式加入成员必须以空阅读位置、空阅读时间和零未读开始");
        return joined;
    }

    /** C2：离开频道并物理删除成员；关系缺失时幂等成功。 */
    public boolean leave(LeaveChatCommand command) {
        require(command != null, CHAT_READ_STATE_INVALID, "离开频道命令不能为空");
        requireNotBlank(command.userId(), CHAT_READ_STATE_INVALID, "用户编号不能为空");
        ChatMemberState removed = membersByUser.remove(command.userId());
        if (removed != null) {
            removedMembers.add(removed);
        }
        checkInvariants();
        return removed != null;
    }

    /**
     * C3：记录一条已保存的新消息。
     *
     * <p>除发送者外的当前成员未读数各增加一；发送者缺失时补建，存在时只向前推进。
     * 方法先验证全部输入与可变更状态，避免拒绝命令留下部分内存变更。</p>
     */
    public ChatMemberState recordNewMessage(
            RecordNewMessageCommand command, ChatMemberIdGenerator idGenerator) {
        require(command != null, CHAT_READ_STATE_INVALID, "记录新消息命令不能为空");
        requireComparableId(command.messageId());
        requireNotBlank(command.senderUserId(), CHAT_READ_STATE_INVALID, "发送者编号不能为空");
        require(command.messagesAfterPosition() >= 0,
                CHAT_READ_STATE_INVALID,
                "消息位置之后的消息数不能为负");

        Map<String, ChatMemberState> before = new LinkedHashMap<>(membersByUser);
        ChatMemberState senderBefore = before.get(command.senderUserId());
        int senderComparison = senderBefore == null
                ? 1
                : comparePositions(command.messageId(), senderBefore.lastReadMessageId());
        require(senderComparison >= 0,
                CHAT_READ_POSITION_REGRESSION,
                "新消息编号不得早于发送者当前已读位置");

        // 先检查所有增量均不会溢出，保证命令失败时聚合不发生部分变化。
        for (ChatMemberState member : before.values()) {
            if (!Objects.equals(member.userId(), command.senderUserId())) {
                try {
                    Math.addExact(member.unreadCount(), 1);
                } catch (ArithmeticException e) {
                    throw new ChatMemberDomainException(
                            CHAT_READ_STATE_INVALID, "成员未读数递增溢出", e);
                }
            }
        }

        for (ChatMemberState member : before.values()) {
            if (!Objects.equals(member.userId(), command.senderUserId())) {
                membersByUser.put(member.userId(), member.incrementUnread());
            }
        }

        ChatMemberState senderAfter;
        if (senderBefore == null) {
            senderAfter = ChatMemberState.repaired(
                    nextUniqueMemberId(idGenerator),
                    refId,
                    command.senderUserId(),
                    command.messageId(),
                    null,
                    command.messagesAfterPosition());
            membersByUser.put(senderAfter.userId(), senderAfter);
        } else if (senderComparison > 0) {
            senderAfter = senderBefore.advancePosition(
                    command.messageId(),
                    senderBefore.lastReadTime(),
                    command.messagesAfterPosition());
            membersByUser.put(senderAfter.userId(), senderAfter);
        } else {
            senderAfter = senderBefore;
        }

        checkInvariants();
        checkNewMessageInvariant(before, senderAfter, command);
        return senderAfter;
    }

    /** C4：确认一次非空交付批次；旧位置或相等位置不得改写阅读状态。 */
    public ChatMemberState confirmDeliveredMessage(
            ConfirmDeliveredMessageCommand command, ChatMemberIdGenerator idGenerator) {
        require(command != null, CHAT_READ_STATE_INVALID, "确认交付命令不能为空");
        requireNotBlank(command.userId(), CHAT_READ_STATE_INVALID, "用户编号不能为空");
        requireComparableId(command.latestMessageId());
        require(command.messagesAfterPosition() >= 0,
                CHAT_READ_STATE_INVALID,
                "剩余未读数不能为负");

        ChatMemberState current = membersByUser.get(command.userId());
        ChatMemberState result;
        if (current == null) {
            result = ChatMemberState.repaired(
                    nextUniqueMemberId(idGenerator),
                    refId,
                    command.userId(),
                    command.latestMessageId(),
                    command.readAt(),
                    command.messagesAfterPosition());
            membersByUser.put(result.userId(), result);
        } else {
            int comparison = comparePositions(command.latestMessageId(), current.lastReadMessageId());
            if (comparison > 0) {
                result = current.advancePosition(
                        command.latestMessageId(),
                        command.readAt(),
                        command.messagesAfterPosition());
                membersByUser.put(result.userId(), result);
            } else {
                result = current;
            }
        }

        checkInvariants();
        return result;
    }

    public String refId() {
        return refId;
    }

    /** 当前成员集合的不可变快照，供仓储批量保存。 */
    public List<ChatMemberState> members() {
        return List.copyOf(membersByUser.values());
    }

    /** C2 后应在同一事务内物理删除的成员快照。 */
    public List<ChatMemberState> removedMembers() {
        return List.copyOf(removedMembers);
    }

    /** 将数据库 {@code uk_ref_user} 冲突转换为 I1 的稳定领域错误。 */
    public static ChatMemberDomainException duplicateMember(Throwable cause) {
        return new ChatMemberDomainException(
                CHAT_MEMBER_DUPLICATE, "同一频道与用户最多只能存在一条成员记录", cause);
    }

    /** I1-I4：每个成功命令完成后校验与当前状态相关的不变量。 */
    private void checkInvariants() {
        Map<String, String> usersByBusinessId = new LinkedHashMap<>();
        for (Map.Entry<String, ChatMemberState> entry : membersByUser.entrySet()) {
            ChatMemberState member = entry.getValue();
            requireNotBlank(member.bizId(), CHAT_MEMBER_DUPLICATE, "成员业务编号不能为空");
            requireNotBlank(member.userId(), CHAT_MEMBER_DUPLICATE, "成员用户编号不能为空");
            require(Objects.equals(entry.getKey(), member.userId()),
                    CHAT_MEMBER_DUPLICATE,
                    "成员索引与用户编号不一致");
            require(Objects.equals(refId, member.refId()),
                    CHAT_MEMBER_DUPLICATE,
                    "成员不属于当前频道");
            String previousUser = usersByBusinessId.putIfAbsent(member.bizId(), member.userId());
            require(previousUser == null,
                    CHAT_MEMBER_DUPLICATE,
                    "频道内成员业务编号必须唯一");
            require(member.unreadCount() >= 0,
                    CHAT_READ_STATE_INVALID,
                    "成员未读数不能为负");
            if (!isBlank(member.lastReadMessageId())) {
                requireComparableId(member.lastReadMessageId());
            }
        }
    }

    /** I4：C3 后逐个核对其他成员增量以及发送者的补建或位置校准结论。 */
    private void checkNewMessageInvariant(
            Map<String, ChatMemberState> before,
            ChatMemberState senderAfter,
            RecordNewMessageCommand command) {
        for (ChatMemberState previous : before.values()) {
            ChatMemberState current = membersByUser.get(previous.userId());
            if (Objects.equals(previous.userId(), command.senderUserId())) {
                continue;
            }
            require(current != null
                            && current.unreadCount() == previous.unreadCount() + 1
                            && Objects.equals(current.lastReadMessageId(), previous.lastReadMessageId())
                            && Objects.equals(current.lastReadTime(), previous.lastReadTime()),
                    CHAT_UNREAD_DIVERGED,
                    "非发送者的未读递增或阅读位置发生分歧");
        }

        require(senderAfter != null
                        && Objects.equals(senderAfter.userId(), command.senderUserId()),
                CHAT_UNREAD_DIVERGED,
                "发送者成员关系未建立");
        if (!before.containsKey(command.senderUserId())) {
            require(Objects.equals(senderAfter.lastReadMessageId(), command.messageId())
                            && senderAfter.lastReadTime() == null
                            && senderAfter.unreadCount() == command.messagesAfterPosition(),
                    CHAT_UNREAD_DIVERGED,
                    "缺失发送者的补建阅读状态不正确");
        }
    }

    private String nextUniqueMemberId(ChatMemberIdGenerator idGenerator) {
        require(idGenerator != null, CHAT_MEMBER_DUPLICATE, "成员编号生成器不能为空");
        String memberId = idGenerator.nextMemberId();
        requireNotBlank(memberId, CHAT_MEMBER_DUPLICATE, "生成的成员业务编号不能为空");
        boolean duplicate = membersByUser.values().stream()
                .anyMatch(member -> Objects.equals(member.bizId(), memberId));
        require(!duplicate, CHAT_MEMBER_DUPLICATE, "生成的成员业务编号已存在");
        return memberId;
    }

    private static int comparePositions(String candidate, String current) {
        requireComparableId(candidate);
        if (isBlank(current)) {
            return 1;
        }
        requireComparableId(current);
        require(candidate.length() == current.length(),
                CHAT_READ_POSITION_REGRESSION,
                "消息编号长度不同，无法按雪花编号顺序比较");
        return candidate.compareTo(current);
    }

    private static void requireComparableId(String messageId) {
        boolean comparable = !isBlank(messageId);
        if (comparable) {
            for (int index = 0; index < messageId.length(); index++) {
                if (messageId.charAt(index) < '0' || messageId.charAt(index) > '9') {
                    comparable = false;
                    break;
                }
            }
        }
        require(comparable,
                CHAT_READ_POSITION_REGRESSION,
                "消息编号必须是可比较的十进制雪花编号");
    }

    private static void requireNotBlank(String value, String errorIdentifier, String message) {
        require(!isBlank(value), errorIdentifier, message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw error(errorIdentifier, message);
        }
    }

    private static ChatMemberDomainException error(String errorIdentifier, String message) {
        return new ChatMemberDomainException(errorIdentifier, message);
    }
}
