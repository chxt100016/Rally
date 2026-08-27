package com.rally.domain.identity.userextension;

import java.util.Objects;

/** 由用户编号与扩展键组成的不可变扩展身份。 */
public final class UserExtensionIdentity {

    private final String userId;
    private final String extensionKey;

    private UserExtensionIdentity(String userId, String extensionKey) {
        this.userId = userId;
        this.extensionKey = extensionKey;
    }

    /** I1：用户编号和扩展键必须非空白。 */
    public static UserExtensionIdentity of(String userId, String extensionKey) {
        if (isBlank(userId) || isBlank(extensionKey)) {
            throw new UserExtensionDomainException(
                    UserExtensionError.USER_EXTENSION_KEY_CONFLICT,
                    "用户编号与扩展键不能为空");
        }
        return new UserExtensionIdentity(userId, extensionKey);
    }

    public String getUserId() {
        return userId;
    }

    public String getExtensionKey() {
        return extensionKey;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserExtensionIdentity that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(extensionKey, that.extensionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, extensionKey);
    }
}
