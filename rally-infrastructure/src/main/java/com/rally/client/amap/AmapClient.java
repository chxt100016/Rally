package com.rally.client.amap;

import com.rally.client.amap.convert.AmapPoiConvertMapper;
import com.rally.client.amap.model.AmapPoiResponse;
import com.rally.domain.court.gateway.CourtMapClient;
import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.utils.Http;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 高德地图关键词搜索客户端，CourtMapClient 的高德实现。
 */
@Slf4j
@Component
public class AmapClient implements CourtMapClient {

    private static final String PLACE_TEXT_URL = "https://restapi.amap.com/v5/place/text";
    private static final String SUCCESS_STATUS = "1";

    private final String apiKey;

    public AmapClient(@Value("${amap.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public boolean configured() {
        return StringUtils.isNotBlank(apiKey);
    }

    @Override
    public List<CourtPoi> searchPage(String keyword, String regionCode, int pageNum, int pageSize) {
        try {
            AmapPoiResponse response = Http.uri(PLACE_TEXT_URL)
                    .param("key", apiKey)
                    .param("keywords", keyword)
                    .param("region", regionCode)
                    .param("city_limit", "true")
                    .param("show_fields", "children,business,indoor")
                    .param("page_size", String.valueOf(pageSize))
                    .param("page_num", String.valueOf(pageNum))
                    .doGet()
                    .result(AmapPoiResponse.class);
            if (response == null || !SUCCESS_STATUS.equals(response.getStatus())) {
                log.warn("高德检索返回错误。region={} page={} info={}", regionCode, pageNum, response == null ? "无响应" : response.getInfo());
                return null;
            }
            if (response.getPois() == null || response.getPois().isEmpty()) {
                return List.of();
            }
            return AmapPoiConvertMapper.INSTANCE.toCourtPoiList(response.getPois());
        } catch (Exception e) {
            log.warn("高德检索请求失败。region={} page={}", regionCode, pageNum, e);
            return null;
        }
    }
}
