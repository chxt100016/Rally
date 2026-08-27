package com.rally.domain.identity.userextension;

/** C2 按用户和扩展键删除资料的命令入参。 */
public final class RemoveUserExtensionCommand {

    private final String userId;
    private final String extensionKey;

    public RemoveUserExtensionCommand(String userId, String extensionKey) {
        this.userId = userId;
        this.extensionKey = extensionKey;
    }

    public String getUserId() {
        return userId;
    }

    public String getExtensionKey() {
        return extensionKey;
    }

    public UserExtensionIdentity identity() {
        return UserExtensionIdentity.of(userId, extensionKey);
    }
}
