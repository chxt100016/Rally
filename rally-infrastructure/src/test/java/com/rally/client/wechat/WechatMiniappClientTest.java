package com.rally.client.wechat;

import com.rally.config.WechatAppProperties;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.auth.model.WechatPhoneInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WechatMiniappClientTest {

    private HttpServer server;
    private String tokenResponse;
    private String phoneResponse;
    private WechatMiniappClient client;

    @BeforeEach
    public void setUp() throws IOException {
        tokenResponse = "{\"access_token\":\"access-token\",\"expires_in\":7200,\"errcode\":0}";
        phoneResponse = "{\"errcode\":0,\"phone_info\":{\"phoneNumber\":\"13800138000\",\"purePhoneNumber\":\"13800138000\",\"countryCode\":\"86\"}}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/token", exchange -> respond(exchange, tokenResponse));
        server.createContext("/phone", exchange -> respond(exchange, phoneResponse));
        server.start();
        WechatAppProperties properties = new WechatAppProperties();
        properties.setAppId("app-id");
        properties.setSecret("secret");
        properties.setAccessTokenUrl(baseUrl() + "/token");
        properties.setPhoneNumberUrl(baseUrl() + "/phone");
        client = new WechatMiniappClient(properties, new WechatAccessTokenClient(properties));
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void getPhoneNumberReturnsWechatPhoneInfo() {
        WechatPhoneInfo phoneInfo = client.getPhoneNumber("dynamic-code");

        assertEquals("13800138000", phoneInfo.getPhoneNumber());
        assertEquals("13800138000", phoneInfo.getPurePhoneNumber());
        assertEquals("86", phoneInfo.getCountryCode());
    }

    @Test
    public void getPhoneNumberRejectsInvalidCodeResponse() {
        phoneResponse = "{\"errcode\":40029,\"errmsg\":\"invalid code\"}";

        try {
            client.getPhoneNumber("invalid-code");
        } catch (BusinessException exception) {
            assertEquals(BizErrorCode.WECHAT_PHONE_NUMBER_FAILED, exception.getErrorCode());
            return;
        }
        throw new AssertionError("Expected BusinessException");
    }

    @Test
    public void getPhoneNumberRejectsAccessTokenFailure() {
        tokenResponse = "{\"errcode\":40013,\"errmsg\":\"invalid appid\"}";

        try {
            client.getPhoneNumber("dynamic-code");
        } catch (BusinessException exception) {
            assertEquals(BizErrorCode.WECHAT_AUTH_FAILED, exception.getErrorCode());
            return;
        }
        throw new AssertionError("Expected BusinessException");
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
