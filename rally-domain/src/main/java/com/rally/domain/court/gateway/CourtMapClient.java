package com.rally.domain.court.gateway;

import com.rally.domain.court.model.CourtPoi;

import java.util.List;

/**
 * 地图服务网关：按关键词与行政区划检索球场场所记录。
 */
public interface CourtMapClient {

    /** 调用凭据是否已配置 */
    boolean configured();

    /**
     * 检索一页场所记录。
     *
     * @return 检索失败或地图服务返回业务错误时返回 null；该页没有结果时返回空列表
     */
    List<CourtPoi> searchPage(String keyword, String regionCode, int pageNum, int pageSize);
}
