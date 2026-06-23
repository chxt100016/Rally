package com.rally.domain.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CountryEnum {

    ITA("ITA", "意大利"),
    ESP("ESP", "西班牙"),
    SRB("SRB", "塞尔维亚"),
    GER("GER", "德国"),
    RUS("RUS", "俄罗斯"),
    DNK("DNK", "丹麦"),
    POL("POL", "波兰"),
    USA("USA", "美国"),
    BGR("BGR", "保加利亚"),
    FRA("FRA", "法国"),
    GBR("GBR", "英国"),
    CHE("CHE", "瑞士"),
    GRC("GRC", "希腊"),
    HRV("HRV", "克罗地亚"),
    AUS("AUS", "澳大利亚"),
    CAN("CAN", "加拿大"),
    JPN("JPN", "日本"),
    CHN("CHN", "中国"),
    ARG("ARG", "阿根廷"),
    CHL("CHL", "智利"),
    BEL("BEL", "比利时"),
    NED("NED", "荷兰"),
    KAZ("KAZ", "哈萨克斯坦"),
    CZE("CZE", "捷克"),
    SVK("SVK", "斯洛伐克"),
    NOR("NOR", "挪威"),
    SWE("SWE", "瑞典"),
    FIN("FIN", "芬兰"),
    PRT("PRT", "葡萄牙"),
    BLR("BLR", "白俄罗斯"),
    ROU("ROU", "罗马尼亚"),
    UKR("UKR", "乌克兰"),
    BRA("BRA", "巴西"),
    COL("COL", "哥伦比亚"),
    HUN("HUN", "匈牙利"),
    GEO("GEO", "格鲁吉亚"),
    LAT("LAT", "拉脱维亚"),
    AUT("AUT", "奥地利"),
    RSA("RSA", "南非"),
    TPE("TPE", "中国台湾"),
    THA("THA", "泰国"),
    IND("IND", "印度"),
    MEX("MEX", "墨西哥"),
    TUN("TUN", "突尼斯");

    private final String code;
    private final String name;

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
        CountryVO country = new CountryVO();
        country.setCode(code);
        country.setName(code);
        return country;
    }
}
