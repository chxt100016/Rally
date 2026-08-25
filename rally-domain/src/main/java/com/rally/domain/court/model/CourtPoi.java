package com.rally.domain.court.model;

import lombok.Data;

/**
 * 地图服务返回的场所记录，收录球场的原始素材。
 * 字段保持地图返回的原始形态，不做加工。
 */
@Data
public class CourtPoi {
    /** 场所编号，收录为球场的三方来源编号 */
    private String id;
    private String name;
    private String address;
    /** 经纬度串，形如 "经度,纬度" */
    private String location;
    /** 场所类型串 */
    private String poiType;
    /** 推荐标签 */
    private String rectag;
    /** 关键标签 */
    private String keytag;
    private String rating;
    private String cost;
    private String opentime;
    private String tel;
    /** 区县编码 */
    private String adcode;
    /** 区县名称 */
    private String adname;
}
