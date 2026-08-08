package com.rally.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rally.domain.behavior.model.UserBehaviorLogData;
import com.rally.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class UserBehaviorInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = UserBehaviorInterceptor.class.getName() + ".userId";

    private static final String START_TIME_ATTRIBUTE = UserBehaviorInterceptor.class.getName() + ".startTime";
    private static final String OCCURRED_AT_ATTRIBUTE = UserBehaviorInterceptor.class.getName() + ".occurredAt";
    private static final Set<String> SENSITIVE_PARAM_NAMES = Set.of("code", "phone", "phonenumber", "purephonenumber");

    private final UserBehaviorLogWriter writer;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxParamsBytes;

    public UserBehaviorInterceptor(
            UserBehaviorLogWriter writer,
            ObjectMapper objectMapper,
            @Value("${behavior-log.enabled:true}") boolean enabled,
            @Value("${behavior-log.max-params-bytes:16384}") int maxParamsBytes) {
        this.writer = writer;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxParamsBytes = Math.max(1, maxParamsBytes);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (enabled) {
            request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
            request.setAttribute(OCCURRED_AT_ATTRIBUTE, LocalDateTime.now());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!enabled) {
            return;
        }
        try {
            UserBehaviorLogData data = buildLog(request, response, ex);
            writer.submit(data);
        } catch (RuntimeException exception) {
            // Behavior collection is best effort and must never change the business response.
            log.warn("Failed to collect user behavior log", exception);
        }
    }

    private UserBehaviorLogData buildLog(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        long now = System.currentTimeMillis();
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        Object occurredAt = request.getAttribute(OCCURRED_AT_ATTRIBUTE);

        UserBehaviorLogData logPO = new UserBehaviorLogData();
        Object requestUserId = request.getAttribute(USER_ID_ATTRIBUTE);
        logPO.setUserId(requestUserId == null ? UserContext.getIfPresent() : requestUserId.toString());
        logPO.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        logPO.setHttpMethod(truncate(request.getMethod(), 10));
        logPO.setRequestUri(truncate(request.getServletPath(), 512));
        logPO.setRoutePattern(truncate(attributeText(request, HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), 255));

        CapturedParams capturedParams = captureParams(request);
        logPO.setRequestParams(capturedParams.json());
        logPO.setParamsTruncated(capturedParams.truncated());

        logPO.setClientIp(truncate(getClientIp(request), 45));
        logPO.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
        logPO.setHttpStatus(response.getStatus());
        logPO.setDurationMs(startTime instanceof Long ? Math.max(0L, now - (Long) startTime) : 0L);
        logPO.setExceptionType(ex == null ? null : truncate(ex.getClass().getName(), 255));
        logPO.setOccurredAt(occurredAt instanceof LocalDateTime ? (LocalDateTime) occurredAt : LocalDateTime.now());
        return logPO;
    }

    private CapturedParams captureParams(HttpServletRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("path", pathParams(request));

        Set<String> queryNames = queryParameterNames(request.getQueryString());
        params.put("query", parameterValues(request, queryNames));

        boolean truncated = false;
        Object body = Collections.emptyMap();
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            body = multipartBody(multipartRequest, queryNames);
        } else if (isFormRequest(request)) {
            body = formBody(request, queryNames);
        } else if (request instanceof UserBehaviorRequestWrapper wrapper) {
            if (wrapper.isOverflow()) {
                body = omittedBody("payload_too_large", request.getContentLengthLong());
                truncated = true;
            } else {
                body = cachedBody(wrapper);
            }
        }
        params.put("body", body);
        params = redactSensitive(params);

        String json = toJson(params);
        int jsonBytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (jsonBytes > maxParamsBytes) {
            params.put("body", omittedBody("params_too_large", jsonBytes));
            truncated = true;
            json = toJson(params);
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > maxParamsBytes) {
            Map<String, Object> omitted = new LinkedHashMap<>();
            omitted.put("_omitted", "params_too_large");
            omitted.put("_originalBytes", jsonBytes);
            json = toJson(omitted);
        }
        return new CapturedParams(json, truncated);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pathParams(HttpServletRequest request) {
        Object value = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> parameterValues(HttpServletRequest request, Set<String> names) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : names) {
            String[] parameterValues = request.getParameterValues(name);
            if (parameterValues != null) {
                values.put(name, parameterValues.length == 1 ? parameterValues[0] : Arrays.asList(parameterValues));
            }
        }
        return values;
    }

    private Map<String, Object> formBody(HttpServletRequest request, Set<String> queryNames) {
        Set<String> formNames = new LinkedHashSet<>(request.getParameterMap().keySet());
        formNames.removeAll(queryNames);
        return parameterValues(request, formNames);
    }

    private Map<String, Object> multipartBody(MultipartHttpServletRequest request, Set<String> queryNames) {
        Map<String, Object> body = new LinkedHashMap<>(formBody(request, queryNames));
        Map<String, List<Map<String, Object>>> files = new LinkedHashMap<>();
        request.getMultiFileMap().forEach((name, multipartFiles) -> {
            List<Map<String, Object>> metadata = new ArrayList<>();
            for (MultipartFile file : multipartFiles) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("filename", file.getOriginalFilename());
                item.put("contentType", file.getContentType());
                item.put("size", file.getSize());
                metadata.add(item);
            }
            files.put(name, metadata);
        });
        if (!files.isEmpty()) {
            body.put("_files", files);
        }
        return body;
    }

    private Object cachedBody(UserBehaviorRequestWrapper wrapper) {
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return Collections.emptyMap();
        }
        String body = new String(content, wrapper.getCharacterEncoding() == null
                ? StandardCharsets.UTF_8
                : java.nio.charset.Charset.forName(wrapper.getCharacterEncoding()));
        if (isJson(wrapper.getContentType())) {
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
                return jsonNode == null ? Collections.emptyMap() : jsonNode;
            } catch (JsonProcessingException ignored) {
                return body;
            }
        }
        return isText(wrapper.getContentType()) ? body : Collections.emptyMap();
    }

    private Map<String, Object> redactSensitive(Map<?, ?> source) {
        Map<String, Object> redacted = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String name = String.valueOf(key);
            redacted.put(name, isSensitiveParam(name) ? "***" : redactSensitiveValue(value));
        });
        return redacted;
    }

    private Object redactSensitiveValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return redactSensitive(map);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::redactSensitiveValue).toList();
        }
        if (value instanceof ObjectNode objectNode) {
            ObjectNode redacted = objectNode.deepCopy();
            List<String> names = new ArrayList<>();
            redacted.fieldNames().forEachRemaining(names::add);
            names.forEach(name -> {
                if (isSensitiveParam(name)) {
                    redacted.put(name, "***");
                } else {
                    redacted.set(name, (JsonNode) redactSensitiveValue(redacted.get(name)));
                }
            });
            return redacted;
        }
        if (value instanceof ArrayNode arrayNode) {
            ArrayNode redacted = arrayNode.deepCopy();
            for (int i = 0; i < redacted.size(); i++) {
                redacted.set(i, (JsonNode) redactSensitiveValue(redacted.get(i)));
            }
            return redacted;
        }
        return value;
    }

    private boolean isSensitiveParam(String name) {
        return SENSITIVE_PARAM_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> omittedBody(String reason, long originalBytes) {
        Map<String, Object> omitted = new LinkedHashMap<>();
        omitted.put("_omitted", reason);
        if (originalBytes >= 0) {
            omitted.put("_originalBytes", originalBytes);
        }
        return omitted;
    }

    private Set<String> queryParameterNames(String queryString) {
        if (StringUtils.isBlank(queryString)) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String pair : queryString.split("&")) {
            String encodedName = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;
            try {
                names.add(URLDecoder.decode(encodedName, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                names.add(encodedName);
            }
        }
        return names;
    }

    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("application/x-www-form-urlencoded");
    }

    private boolean isJson(String contentType) {
        return contentType != null && (contentType.toLowerCase().contains("application/json")
                || contentType.toLowerCase().contains("+json"));
    }

    private boolean isText(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase();
        return normalized.startsWith("text/") || normalized.contains("xml");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize behavior request parameters", ex);
        }
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

    private String attributeText(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record CapturedParams(String json, boolean truncated) {
    }
}
