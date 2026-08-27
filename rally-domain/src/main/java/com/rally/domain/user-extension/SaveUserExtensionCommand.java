package com.rally.domain.identity.userextension;

/** C1 保存用户扩展资料的命令入参。 */
public final class SaveUserExtensionCommand {

    private final String userId;
    private final String extensionKey;
    private final String extensionValue;

    public SaveUserExtensionCommand(String userId, String extensionKey, String extensionValue) {
        this.userId = userId;
        this.extensionKey = extensionKey;
        this.extensionValue = extensionValue;
    }

    public String getUserId() {
        return userId;
    }

    public String getExtensionKey() {
        return extensionKey;
    }

    public String getExtensionValue() {
        return extensionValue;
    }

    public UserExtensionIdentity identity() {
        return UserExtensionIdentity.of(userId, extensionKey);
    }
}
