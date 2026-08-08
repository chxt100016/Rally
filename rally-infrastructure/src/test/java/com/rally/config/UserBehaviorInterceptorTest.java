package com.rally.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserBehaviorInterceptorTest {

    @Test
    @SuppressWarnings("unchecked")
    public void redactSensitiveMasksWechatCodeAndPhone() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        UserBehaviorInterceptor interceptor = new UserBehaviorInterceptor(null, objectMapper, true, 16384);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("body", objectMapper.readTree("{\"code\":\"dynamic-code\",\"phoneNumber\":\"13800138000\",\"cityCode\":\"440300\"}"));
        Method method = UserBehaviorInterceptor.class.getDeclaredMethod("redactSensitive", Map.class);
        method.setAccessible(true);

        Map<String, Object> redacted = (Map<String, Object>) method.invoke(interceptor, params);
        JsonNode body = (JsonNode) redacted.get("body");

        assertEquals("***", body.get("code").asText());
        assertEquals("***", body.get("phoneNumber").asText());
        assertEquals("440300", body.get("cityCode").asText());
    }
}
