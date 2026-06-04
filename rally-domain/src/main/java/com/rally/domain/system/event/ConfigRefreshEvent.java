package com.rally.domain.system.event;

import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件，admin 写后本地广播刷新缓存
 */
public class ConfigRefreshEvent extends ApplicationEvent {

    public ConfigRefreshEvent(Object source) {
        super(source);
    }
}
