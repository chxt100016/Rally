package com.rally.tennis.parser;

import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.tennis.model.Discipline;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WTA App 实时比赛解析器（女子单打 LS）
 * 继承 AtpAppLiveMatchParser，复用相同的请求逻辑和比分解析逻辑
 * 仅重写 ls() 方法，过滤 MatchId 以 "LS" 开头的比赛
 * 数据源与 ATP 相同：https://app.atptour.com/api/v2/gateway/livematches
 */
@Component
public class WtaAppLiveMatchParser extends AtpAppLiveMatchParser {

    /**
     * 重写 ls()，从响应中提取女子单打（LS）比赛列表
     * ms() 方法继承自父类但不会被调用（父类 ms() 过滤 "MS" 前缀，此处不需要）
     */
    @Override
    protected List<DrawResult<List<AtpAppLiveResponse.LiveMatch>>> ls(
            AtpAppLiveResponse data, DrawParams params) {
        return buildDrawResult(data, params, "LS", Discipline.SINGLES);
    }

    /**
     * 重写 ms()，WTA 解析器不处理 MS 数据
     */
    @Override
    protected List<DrawResult<List<AtpAppLiveResponse.LiveMatch>>> ms(
            AtpAppLiveResponse data, DrawParams params) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.WTA_APP_LIVE;
    }
}
