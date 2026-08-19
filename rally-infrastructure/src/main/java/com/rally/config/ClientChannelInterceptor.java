package com.rally.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tour.model.Result;
import com.rally.utils.ClientChannelContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 统一解析客户端渠道。渠道只用于业务路由和统计，不作为可信身份凭证。
 */
@Component
public class ClientChannelInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public ClientChannelInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String header = request.getHeader(ClientChannel.HEADER_NAME);
        if (StringUtils.isBlank(header)) {
            ClientChannelContext.set(isWechatSpecificPath(request)
                    ? ClientChannel.WECHAT_MINIAPP
                    : ClientChannel.UNKNOWN);
            return true;
        }

        try {
            ClientChannelContext.set(ClientChannel.parse(header));
            return true;
        } catch (IllegalArgumentException exception) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.fail(BizErrorCode.PARAM_ERROR.getCode(),
                            "不支持的客户端渠道: " + header)));
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ClientChannelContext.clear();
    }

    private boolean isWechatSpecificPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/wechat".equals(path) || path.startsWith("/wechat/");
    }
}
