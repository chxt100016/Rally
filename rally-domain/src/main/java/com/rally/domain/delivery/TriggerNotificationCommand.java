package com.rally.domain.delivery;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** C1 触发业务通知；场景和内容保持渠道无关。 */
public record TriggerNotificationCommand(
        String eventId,
        NotifyBizType bizType,
        String refBizId,
        NoticeScene noticeScene,
        List<String> recipientIds,
        Map<String, Object> data,
        Predicate<String> recipientFilter,
        Set<NotifyChannel> channels) {

    public TriggerNotificationCommand {
        recipientIds = recipientIds == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(recipientIds));
        data = data == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(data));
        channels = channels == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(channels));
    }
}
