package com.rally.utils;

import com.rally.config.ClientChannel;

/**
 * 当前请求的客户端渠道上下文。
 */
public final class ClientChannelContext {

    private static final ThreadLocal<ClientChannel> HOLDER = new ThreadLocal<>();

    private ClientChannelContext() {
    }

    public static void set(ClientChannel channel) {
        HOLDER.set(channel);
    }

    public static ClientChannel get() {
        ClientChannel channel = HOLDER.get();
        return channel == null ? ClientChannel.UNKNOWN : channel;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
