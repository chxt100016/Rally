package com.rally.domain.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class HttpProxyTest {

    private static final String PROXY_URL = "http://lacdqhja:hmp9e1ij0dsu@209.127.138.10:5784";

    @Test
    public void testDirectRequest() {
        String ip = Http.uri("https://icanhazip.com").param("format", "json").doGet().result();
        assertNotNull("直连请求不应为空", ip);
        System.out.println("直连 IP: " + ip);
    }

    @Test
    public void testProxyRequest() {
        String ip = Http.uri("https://icanhazip.com")
                .param("format", "json")
                .proxy(PROXY_URL)
                .doGet()
                .result();
        assertNotNull("代理请求不应为空", ip);
        System.out.println("代理 IP: " + ip);
    }

    @Test
    public void testProxyChangesIp() {
        String directIp = Http.uri("https://icanhazip.com").param("format", "json").doGet().result();
        String proxyIp = Http.uri("https://icanhazip.com")
                .param("format", "json")
                .proxy(PROXY_URL)
                .doGet()
                .result();

        assertNotNull(directIp);
        assertNotNull(proxyIp);
        assertNotEquals("代理后 IP 应与直连不同", directIp, proxyIp);
        System.out.println("直连: " + directIp + " → 代理: " + proxyIp);
    }

    @Test
    public void testProxyWithFourParams() {
        String ip = Http.uri("https://api.ipify.org")
                .param("format", "json")
                .proxy("209.127.138.10", 5784, "lacdqhja", "hmp9e1ij0dsu")
                .doGet()
                .result();
        assertNotNull("四参代理请求不应为空", ip);
        System.out.println("四参代理 IP: " + ip);
    }
}
