package com.rally.domain.meetup.chatmessage;

import java.util.Objects;

/**
 * 聊天消息聚合根。
 *
 * <p>聚合只提供 C1 发布命令。一旦建立，频道、发送者快照、载荷和发布时间
 * 都只能通过不可变状态快照读取，不对外提供更新或删除入口。</p>
 */
public final class ChatMessage {

    public static final String CHAT_MESSAGE_ID_CONFLICT = "CHAT_MESSAGE_ID_CONFLICT";
    public static final String CHAT_MESSAGE_INVALID = "CHAT_MESSAGE_INVALID";
    public static final String CHAT_MESSAGE_IMMUTABLE = "CHAT_MESSAGE_IMMUTABLE";

    private static final int MAX_BUSINESS_ID_LENGTH = 32;

    private final ChatMessageData state;

    private ChatMessage(ChatMessageData state) {
        this.state = state;
    }

    /**
     * C1：发布一条独立消息。
     *
     * <p>编号生成和唯一性预检均在构造状态前完成；持久化时仍必须依赖
     * {@code uk_biz_id} 抵御并发竞争。</p>
     */
    public static ChatMessage publish(
            PublishChatMessageCommand command,
            ChatMessageIdGenerator idGenerator,
            ChatMessageBusinessIdUniqueness businessIdUniqueness) {
        require(command != null, CHAT_MESSAGE_INVALID, "发布消息命令不能为空");
        require(idGenerator != null, CHAT_MESSAGE_ID_CONFLICT, "消息编号生成器不能为空");
        require(businessIdUniqueness != null,
                CHAT_MESSAGE_ID_CONFLICT,
                "消息编号唯一性检查器不能为空");

        validateCommand(command);
        ChatMessageContentType contentType = ChatMessageContentType.fromCode(command.contentType());
        String messageId = idGenerator.nextMessageId();
        validateMessageId(messageId);
        if (businessIdUniqueness.exists(messageId)) {
            throw idConflict();
        }

        ChatMessage message = new ChatMessage(
                ChatMessageData.newPublished(messageId, command, contentType));
        message.checkInvariants();
        return message;
    }

    /** 从仓储中的一条完整记录恢复已发布消息。 */
    public static ChatMessage restore(ChatMessageData state) {
        require(state != null, CHAT_MESSAGE_IMMUTABLE, "聊天消息状态不能为空");
        ChatMessage message = new ChatMessage(state);
        message.checkInvariants();
        return message;
    }

    /** 交由持久化适配器 insert 的不可变状态快照。 */
    public ChatMessageData state() {
        return state;
    }

    public String messageId() {
        return state.bizId();
    }

    public ChatMessageStatus status() {
        return ChatMessageStatus.PUBLISHED;
    }

    /** 将数据库 {@code uk_biz_id} 冲突转换为 I1 规定的稳定领域错误。 */
    public static ChatMessageDomainException idConflict() {
        return new ChatMessageDomainException(
                CHAT_MESSAGE_ID_CONFLICT,
                "消息业务编号已存在");
    }

    public static ChatMessageDomainException idConflict(Throwable cause) {
        return new ChatMessageDomainException(
                CHAT_MESSAGE_ID_CONFLICT,
                "消息业务编号已存在",
                cause);
    }

    /** I1-I3：C1 完成后校验全部相关不变量。 */
    private void checkInvariants() {
        // I1：业务编号非空且可作为固定宽度雪花编号；全局唯一由预检与数据库唯一键共同保证。
        validateMessageId(state.bizId());

        // I2：归属、作者、内容和可解释的类型在同一次建立时确定。
        requireNotBlank(state.refId(), CHAT_MESSAGE_INVALID, "频道编号不能为空");
        requireNotBlank(state.senderId(), CHAT_MESSAGE_INVALID, "发送者编号不能为空");
        requireNotBlank(state.content(), CHAT_MESSAGE_INVALID, "消息内容不能为空");
        require(state.contentType() != null, CHAT_MESSAGE_INVALID, "消息类型非法");

        // I3：发送者快照可为空字符串但不得缺失；不可变状态与无修改命令保证历史事实不被改写。
        require(state.senderName() != null, CHAT_MESSAGE_IMMUTABLE, "发送者昵称快照不能缺失");
        require(state.senderAvatar() != null, CHAT_MESSAGE_IMMUTABLE, "发送者头像快照不能缺失");
        require(state.createTime() != null, CHAT_MESSAGE_IMMUTABLE, "消息发布时间不能缺失");
        require(state.updateTime() != null, CHAT_MESSAGE_IMMUTABLE, "消息更新时间不能缺失");
    }

    private static void validateCommand(PublishChatMessageCommand command) {
        requireNotBlank(command.refId(), CHAT_MESSAGE_INVALID, "频道编号不能为空");
        requireNotBlank(command.senderId(), CHAT_MESSAGE_INVALID, "发送者编号不能为空");
        requireNotBlank(command.content(), CHAT_MESSAGE_INVALID, "消息内容不能为空");
        require(command.publishedAt() != null, CHAT_MESSAGE_INVALID, "消息发布时间不能为空");
    }

    private static void validateMessageId(String messageId) {
        requireNotBlank(messageId, CHAT_MESSAGE_ID_CONFLICT, "消息业务编号不能为空");
        require(messageId.length() <= MAX_BUSINESS_ID_LENGTH,
                CHAT_MESSAGE_ID_CONFLICT,
                "消息业务编号超出存储长度");
        for (int index = 0; index < messageId.length(); index++) {
            char current = messageId.charAt(index);
            require(current >= '0' && current <= '9',
                    CHAT_MESSAGE_ID_CONFLICT,
                    "消息业务编号必须是十进制雪花编号");
        }
    }

    private static void requireNotBlank(String value, String errorIdentifier, String message) {
        require(value != null && !value.trim().isEmpty(), errorIdentifier, message);
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new ChatMessageDomainException(Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
