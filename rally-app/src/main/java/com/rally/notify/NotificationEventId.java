package com.rally.notify;

import com.rally.domain.notify.enums.NoticeScene;

import java.util.Arrays;
import java.util.stream.Collectors;

/** 业务触达事件标识构造器。 */
public final class NotificationEventId {

    private NotificationEventId() {
    }

    public static String of(NoticeScene scene, Object... sourceParts) {
        String suffix = Arrays.stream(sourceParts)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return scene.name() + ":" + suffix;
    }
}
