package com.rally.config;

import java.util.Locale;

/**
 * 调用方渠道。通用接口通过 {@link #HEADER_NAME} 传递，不参与用户身份鉴权。
 */
public enum ClientChannel {
    WECHAT_MINIAPP,
    WEB,
    APP,
    UNKNOWN;

    public static final String HEADER_NAME = "X-Client-Channel";

    public static ClientChannel parse(String value) {
        return ClientChannel.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
