package com.rally.domain.utils;

public class HttpProxyTest {

    public static void main(String[] args) {
        String result = Http.uri("http://www.baidu.com")
                .proxy("http://lacdqhja:hmp9e1ij0dsu@209.127.138.10:5784")
                .doGet()
                .result();

        System.out.println("status: " + (result != null ? "success" : "failed"));
        if (result != null) {
            System.out.println("length: " + result.length());
            System.out.println("preview: " + result.substring(0, Math.min(200, result.length())));
        }
    }
}
