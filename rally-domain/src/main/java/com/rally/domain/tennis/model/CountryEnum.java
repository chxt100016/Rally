package com.rally.domain.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 国家代码枚举
 */
@Getter
@AllArgsConstructor
public enum CountryEnum {

    IT("IT", "意大利"),
    ES("ES", "西班牙"),
    RS("RS", "塞尔维亚"),
    DE("DE", "德国"),
    RU("RU", "俄罗斯"),
    DK("DK", "丹麦"),
    PL("PL", "波兰"),
    US("US", "美国"),
    BG("BG", "保加利亚"),
    FR("FR", "法国"),
    GB("GB", "英国"),
    CH("CH", "瑞士"),
    GR("GR", "希腊"),
    HR("HR", "克罗地亚"),
    AU("AU", "澳大利亚"),
    CA("CA", "加拿大"),
    JP("JP", "日本"),
    CN("CN", "中国"),
    AR("AR", "阿根廷"),
    CL("CL", "智利"),
    BE("BE", "比利时"),
    NL("NL", "荷兰"),
    SR("SR", "塞尔维亚"),
    KZ("KZ", "哈萨克斯坦"),
    CZ("CZ", "捷克"),
    SK("SK", "斯洛伐克"),
    NO("NO", "挪威"),
    SE("SE", "瑞典"),
    FI("FI", "芬兰"),
    PT("PT", "葡萄牙");

    private final String code;
    private final String name;

    /**
     * 根据国家代码获取国家信息
     * @param code 国家代码
     * @return 国家信息，找不到时返回默认值
     */
    public static CountryVO getCountry(String code) {
        if (code == null) {
            return null;
        }
        for (CountryEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                CountryVO country = new CountryVO();
                country.setCode(e.code);
                country.setName(e.name);
                return country;
            }
        }
        // 找不到时返回 code 作为 name
        CountryVO country = new CountryVO();
        country.setCode(code);
        country.setName(code);
        return country;
    }
}
