package com.rally.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.AuthException;
import com.rally.domain.auth.model.TokenPayload;
import com.rally.domain.behavior.model.UserBehaviorLogData;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.UserData;
import com.rally.utils.TokenUtils;
import com.rally.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 根据系统配置中的用户 ID、客户端 IP 和手机号封禁名单拒绝请求。 */
@Slf4j
@Component
public class AccessBlockInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserBehaviorLogWriter behaviorLogWriter;

    private volatile BlocklistSnapshot blockedUsers = BlocklistSnapshot.empty();
    private volatile BlocklistSnapshot blockedClientIps = BlocklistSnapshot.empty();
    private volatile BlocklistSnapshot blockedPhones = BlocklistSnapshot.empty();

    public AccessBlockInterceptor(
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserBehaviorLogWriter behaviorLogWriter) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.behaviorLogWriter = behaviorLogWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientIp = getClientIp(request);
        String userId = getUserId(request);
        if (userId != null) {
            request.setAttribute(UserBehaviorInterceptor.USER_ID_ATTRIBUTE, userId);
        }
        BlocklistSnapshot ipSnapshot = currentSnapshot(SystemConfigKey.ACCESS_BLOCKED_CLIENT_IPS);
        if (ipSnapshot.remarks().containsKey(clientIp)) {
            String remark = ipSnapshot.remarks().get(clientIp);
            recordBlockedRequest(request, userId, clientIp, "CLIENT_IP", remark);
            log.warn("拒绝封禁 IP 访问: clientIp={}, remark={}",
                    clientIp, remark);
            throw new AuthException(BizErrorCode.ACCESS_DENIED);
        }

        BlocklistSnapshot userSnapshot = currentSnapshot(SystemConfigKey.ACCESS_BLOCKED_USER_IDS);
        if (userId != null && userSnapshot.remarks().containsKey(userId)) {
            String remark = userSnapshot.remarks().get(userId);
            recordBlockedRequest(request, userId, clientIp, "USER_ID", remark);
            log.warn("拒绝封禁用户访问: userId={}, remark={}",
                    userId, remark);
            throw new AuthException(BizErrorCode.ACCESS_DENIED);
        }

        BlocklistSnapshot phoneSnapshot = currentSnapshot(SystemConfigKey.ACCESS_BLOCKED_PHONES);
        if (userId != null && !phoneSnapshot.remarks().isEmpty()) {
            String phone = userRepository.findByUserId(userId)
                    .map(UserData::getPhone)
                    .orElse(null);
            if (phone != null && phoneSnapshot.remarks().containsKey(phone)) {
                String remark = phoneSnapshot.remarks().get(phone);
                recordBlockedRequest(request, userId, clientIp, "PHONE", remark);
                log.warn("拒绝封禁手机号访问: userId={}, phone={}, remark={}",
                        userId, maskPhone(phone), remark);
                throw new AuthException(BizErrorCode.ACCESS_DENIED);
            }
        }
        return true;
    }

    private void recordBlockedRequest(
            HttpServletRequest request,
            String userId,
            String clientIp,
            String dimension,
            String remark) {
        try {
            Map<String, Object> blockInfo = new LinkedHashMap<>();
            blockInfo.put("dimension", dimension);
            blockInfo.put("remark", remark);

            UserBehaviorLogData data = new UserBehaviorLogData()
                    .setUserId(userId)
                    .setRequestId(UUID.randomUUID().toString().replace("-", ""))
                    .setHttpMethod(truncate(request.getMethod(), 10))
                    .setRequestUri(truncate(request.getServletPath(), 512))
                    .setRoutePattern(truncate(attributeText(
                            request, HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), 255))
                    .setRequestParams(objectMapper.writeValueAsString(
                            Map.of("_accessBlock", blockInfo)))
                    .setParamsTruncated(false)
                    .setClientIp(truncate(clientIp, 45))
                    .setUserAgent(truncate(request.getHeader("User-Agent"), 512))
                    .setHttpStatus(HttpServletResponse.SC_FORBIDDEN)
                    .setDurationMs(0L)
                    .setExceptionType("ACCESS_BLOCKED_" + dimension)
                    .setOccurredAt(LocalDateTime.now());
            behaviorLogWriter.submit(data);
        } catch (Exception exception) {
            log.warn("记录封禁请求失败: dimension={}", dimension, exception);
        }
    }

    private String getUserId(HttpServletRequest request) {
        String userId = UserContext.getIfPresent();
        if (userId != null) {
            return userId;
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return TokenUtils.verify(authHeader.substring(7))
                .map(payload -> rememberVerifiedPayload(request, payload))
                .map(TokenPayload::getUserId)
                .orElse(null);
    }

    private TokenPayload rememberVerifiedPayload(HttpServletRequest request, TokenPayload payload) {
        request.setAttribute(AuthInterceptor.VERIFIED_TOKEN_PAYLOAD_ATTRIBUTE, payload);
        return payload;
    }

    private BlocklistSnapshot currentSnapshot(SystemConfigKey key) {
        String source = SystemConfig.getString(key.getKey());
        BlocklistSnapshot current = snapshot(key);
        if (Objects.equals(source, current.source())) {
            return current;
        }
        synchronized (this) {
            source = SystemConfig.getString(key.getKey());
            current = snapshot(key);
            if (!Objects.equals(source, current.source())) {
                current = parseSnapshot(key, source);
                setSnapshot(key, current);
            }
            return current;
        }
    }

    private BlocklistSnapshot parseSnapshot(SystemConfigKey key, String source) {
        Map<String, String> remarks = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(source);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("封禁配置不是 JSON 对象");
            }
            root.fields().forEachRemaining(entry -> {
                JsonNode item = entry.getValue();
                JsonNode remark = item == null ? null : item.get("remark");
                if (item == null || !item.isObject() || remark == null || !remark.isTextual()) {
                    throw new IllegalArgumentException("封禁配置项缺少 remark");
                }
                remarks.put(entry.getKey(), remark.textValue());
            });
        } catch (Exception exception) {
            log.error("解析访问封禁配置失败: key={}", key.getKey(), exception);
            remarks.clear();
        }
        return new BlocklistSnapshot(source, Map.copyOf(remarks));
    }

    private BlocklistSnapshot snapshot(SystemConfigKey key) {
        return switch (key) {
            case ACCESS_BLOCKED_USER_IDS -> blockedUsers;
            case ACCESS_BLOCKED_CLIENT_IPS -> blockedClientIps;
            case ACCESS_BLOCKED_PHONES -> blockedPhones;
            default -> throw new IllegalArgumentException("不支持的封禁配置: " + key.getKey());
        };
    }

    private void setSnapshot(SystemConfigKey key, BlocklistSnapshot snapshot) {
        switch (key) {
            case ACCESS_BLOCKED_USER_IDS -> blockedUsers = snapshot;
            case ACCESS_BLOCKED_CLIENT_IPS -> blockedClientIps = snapshot;
            case ACCESS_BLOCKED_PHONES -> blockedPhones = snapshot;
            default -> throw new IllegalArgumentException("不支持的封禁配置: " + key.getKey());
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String attributeText(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    private record BlocklistSnapshot(String source, Map<String, String> remarks) {

        private static BlocklistSnapshot empty() {
            return new BlocklistSnapshot(null, Map.of());
        }
    }
}
