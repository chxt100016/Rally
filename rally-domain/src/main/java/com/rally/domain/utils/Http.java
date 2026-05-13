package com.rally.domain.utils;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.mime.MultipartEntityBuilder;


@Slf4j
public class Http {

    private static final CloseableHttpClient httpClient;

    static {
        RequestConfig config = RequestConfig.custom()
//                .setConnectionRequestTimeout(1000)
//                .setConnectTimeout(5000)
//                .setSocketTimeout(5000)
                .build();

        httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
    }




    private Object entity;

    private HashMap<String, String> entityMap;

    private String fileName;

    private byte[] fileData;

    private String uri;

    private volatile Map<String, String> header;

    private volatile Map<String, String> responseHeaders;

    private volatile List<NameValuePair> param;

    private HttpRequestBase request;

    private StatusLine statusLine;

    private byte[] contentByteArray;

    private boolean success;

    private boolean formEncoded;


    private Http() {
    }


    public static Http uri(String uri) {
        Http http = new Http();
        http.uri = uri;
        return http;
    }

    public Http param(String name, String value){
        if (this.param == null) {
            synchronized (this) {
                if (this.param == null) {
                    this.param = new ArrayList<>();
                }
            }
        }
        this.param.add(new BasicNameValuePair(name, value));
        return this;
    }

    public Http file(String name, byte[] data) {
        this.fileName = name;
        this.fileData = data;
        return this;
    }

    public Http entity(Object entity) {
        this.entity = entity;
        return this;
    }

    public Http entity(String key, String value) {
        if (this.entityMap == null) this.entityMap = new HashMap<>();
        entityMap.put(key, value);
        return this;
    }

    public Http header(String name, String value) {
        if (this.header == null) {
            synchronized (this) {
                if (this.header == null) {
                    this.header = new HashMap<>();
                }
            }
        }
        this.header.put(name, value);
        return this;
    }

    public Http multiPartHeader(){
        this.header("Content-Type", "multipart/form-data");
        return this;
    }

    public Http jsonHeader(){
        this.header("Content-Type", "application/json");
        return this;
    }

    public Http formEncoded(){
        this.formEncoded = true;
        this.header("Content-Type", "application/x-www-form-urlencoded");
        return this;
    }

    public Http header(Map<String, String> header) {
        for (Map.Entry<String, String> entry : header.entrySet()) {
            this.header(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @SneakyThrows
    public <T> T result(Class<T> clazz) {
        if (!this.success || this.contentByteArray == null) {
            return null;
        }
        return StringUtils.isBlank(new String(this.contentByteArray)) ? null : JSON.parseObject(new String(this.contentByteArray), clazz);
    }

    @SneakyThrows
    public <T> T result(TypeReference<T> typeReference) {
        if (!this.success || this.contentByteArray == null) {
            return null;
        }
        return StringUtils.isBlank(new String(this.contentByteArray)) ? null : JSON.parseObject(new String(this.contentByteArray), typeReference);
    }

    @SneakyThrows
    public <T> List<T> resultArray(Class<T> clazz) {
        if (!this.success || this.contentByteArray == null) {
            return null;
        }
        return StringUtils.isBlank(new String(this.contentByteArray)) ? null : JSON.parseArray(new String(this.contentByteArray), clazz);
    }

    @SneakyThrows
    public String result() {
        if (!this.success || this.contentByteArray == null) {
            return null;
        }
        return new String(this.contentByteArray);
    }

    @SneakyThrows
    public void download(String path){
        if (!this.success || this.contentByteArray == null) {
            throw new IOException("HTTP request failed or no content available for download");
        }
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(this.contentByteArray), file);
    }

    @SneakyThrows
    public byte[] byteArray(){
        if (!this.success) {
            return null;
        }
        return this.contentByteArray;
    }


    public int getStatusCode() {
        return this.statusLine.getStatusCode();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getResponseHeader(String name) {
        if (this.responseHeaders == null) return null;
        return this.responseHeaders.get(name);
    }

    @SneakyThrows
    public Http doPost() {
        return this.doPost(null);
    }

    @SneakyThrows
    public Http doPost(PostProcessor postProcessor) {
        this.request = new HttpPost();
        this.execute(postProcessor);
        return this;
    }

    @SneakyThrows
    public Http doPut() {
        return this.doPut(null);
    }

    @SneakyThrows
    public Http doPut(PostProcessor postProcessor) {
        this.request = new HttpPut();
        this.execute(postProcessor);
        return this;
    }

    @SneakyThrows
    public Http doGet(){
        return this.doGet(null);
    }

    @SneakyThrows
    public Http doGet(PostProcessor postProcessor){
        this.request = new HttpGet();
        this.execute(postProcessor);
        return this;
    }

    @SneakyThrows
    public Http doDelete() {
        return this.doDelete(null);
    }

    @SneakyThrows
    public Http doDelete(PostProcessor postProcessor) {
        this.request = new HttpDelete();
        this.execute(postProcessor);
        return this;
    }

    @SneakyThrows
    private void execute(PostProcessor postProcessor){
        this.setHeader();
        this.setUri();
        this.setEntity();

        CloseableHttpResponse response = null;
        try {
            response = Http.httpClient.execute(this.request);

            if(postProcessor != null) {
                CloseableHttpResponse responseTmp = postProcessor.process(Http.httpClient, this.request, response);
                if (response != responseTmp) {
                    response.close();
                    response = responseTmp;
                }
            }

            this.statusLine = response.getStatusLine();
            if (response.getEntity() != null) {
                this.contentByteArray = EntityUtils.toByteArray(response.getEntity());
            } else {
                this.contentByteArray = new byte[0];
            }

            // Capture response headers
            this.responseHeaders = new HashMap<>();
            for (org.apache.http.Header h : response.getAllHeaders()) {
                this.responseHeaders.put(h.getName(), h.getValue());
            }

            this.success = true;

        } catch (Exception e) {
            log.error("http 请求异常，url:{}, entity:{}",
                this.request.getURI() != null ? this.request.getURI().toString() : "unknown",
                this.entity, e);
            this.contentByteArray = null;
            this.success = false;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.warn("关闭 response 失败", e);
                }
            }
            this.request.releaseConnection();
        }
    }

    private void setHeader(){
        if (this.header != null) {
            for (Map.Entry<String, String> e : this.header.entrySet()) {
                this.request.setHeader(e.getKey(), e.getValue());
            }
        }
    }

    @SneakyThrows
    private void setUri(){
        URIBuilder uriBuilder = new URIBuilder(this.uri);
        if (this.param != null) {
            uriBuilder.setParameters(this.param);
        }
        URI uri = uriBuilder.build();
        this.request.setURI(uri);
    }

    private void setEntity(){

        if (!(this.request instanceof HttpGet) && !(this.request instanceof HttpDelete)) {
            if (this.fileName != null && this.fileData != null) {
                MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .addBinaryBody("media", this.fileData, ContentType.MULTIPART_FORM_DATA, this.fileName);
                ((HttpEntityEnclosingRequestBase) this.request).setEntity(builder.build());
            } else if (this.formEncoded && this.entityMap != null) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : this.entityMap.entrySet()) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8))
                      .append("=")
                      .append(java.net.URLEncoder.encode(entry.getValue(), java.nio.charset.StandardCharsets.UTF_8));
                }
                ((HttpEntityEnclosingRequestBase) this.request).setEntity(new StringEntity(sb.toString(), "UTF-8"));
            } else if (this.entityMap != null && this.entity == null) {
                ((HttpEntityEnclosingRequestBase) this.request).setEntity(new StringEntity(JSON.toJSONString(this.entityMap), ContentType.APPLICATION_JSON));
            } else if (this.entity != null) {
                ((HttpEntityEnclosingRequestBase) this.request).setEntity(new StringEntity(JSON.toJSONString(this.entity), ContentType.APPLICATION_JSON));
            }
        }
    }



    public interface PostProcessor {

        CloseableHttpResponse process(CloseableHttpClient client, HttpRequestBase request, CloseableHttpResponse response) throws IOException;
    }

}
