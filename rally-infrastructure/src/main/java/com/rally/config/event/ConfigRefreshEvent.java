package com.rally.config.event;

import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件，admin 写后本地广播刷新
 */
public class ConfigRefreshEvent extends ApplicationEvent {

    public ConfigRefreshEvent(Object source) {
        super(source);
    }
}
