package com.rally.domain.translation.gateway;

import com.rally.domain.translation.model.TranslationData;

import java.util.List;

public interface TranslationClient {

    /**
     * 批量翻译，返回与入参顺序一一对应的译文列表；
     * 调用失败或行数不匹配时返回 null
     */
    List<String> translate(List<TranslationData> tasks);
}
