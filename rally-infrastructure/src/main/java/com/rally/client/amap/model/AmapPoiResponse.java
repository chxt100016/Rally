package com.rally.client.amap.model;

import lombok.Data;

import java.util.List;

/**
 * 高德关键词搜索接口响应体
 */
@Data
public class AmapPoiResponse {

    /** "1" 表示成功 */
    private String status;
    private String info;
    private String infocode;
    private List<AmapPoi> pois;

    @Data
    public static class AmapPoi {
        private String id;
        private String name;
        private String address;
        /** 经纬度串，形如 "经度,纬度" */
        private String location;
        private String type;
        private String adcode;
        private String adname;
        private String citycode;
        private String cityname;
        private AmapBusiness business;
    }

    @Data
    public static class AmapBusiness {
        private String rectag;
        private String keytag;
        private String rating;
        private String cost;
        private String opentime_week;
        private String tel;
    }
}
