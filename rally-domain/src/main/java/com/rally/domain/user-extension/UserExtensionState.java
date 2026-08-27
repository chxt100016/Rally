package com.rally.domain.identity.userextension;

import java.time.LocalDateTime;

/** {@code user_ext} 单条记录对应的不可变聚合状态。 */
public record UserExtensionState(
        Long id,
        String businessId,
        String userId,
        String extensionKey,
        String extensionValue,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static UserExtensionState forSave(Long id,
                                      String businessId,
                                      SaveUserExtensionCommand command) {
        return new UserExtensionState(
                id,
                businessId,
                command.getUserId(),
                command.getExtensionKey(),
                command.getExtensionValue(),
                null,
                null);
    }

}
