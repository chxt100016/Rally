package com.rally.domain.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CountryEnum {

    ITA("ITA", "IT", "意大利"),
    ESP("ESP", "ES", "西班牙"),
    SRB("SRB", "RS", "塞尔维亚"),
    GER("GER", "DE", "德国"),
    RUS("RUS", "RU", "俄罗斯"),
    DNK("DNK", "DK", "丹麦"),
    POL("POL", "PL", "波兰"),
    USA("USA", "US", "美国"),
    BGR("BGR", "BG", "保加利亚"),
    FRA("FRA", "FR", "法国"),
    GBR("GBR", "GB", "英国"),
    CHE("CHE", "CH", "瑞士"),
    SUI("SUI", "CH", "瑞士"),
    GRC("GRC", "GR", "希腊"),
    HRV("HRV", "HR", "克罗地亚"),
    CRO("CRO", "HR", "克罗地亚"),
    AUS("AUS", "AU", "澳大利亚"),
    CAN("CAN", "CA", "加拿大"),
    JPN("JPN", "JP", "日本"),
    CHN("CHN", "CN", "中国"),
    ARG("ARG", "AR", "阿根廷"),
    CHL("CHL", "CL", "智利"),
    BEL("BEL", "BE", "比利时"),
    NED("NED", "NL", "荷兰"),
    NLD("NLD", "NL", "荷兰"),
    KAZ("KAZ", "KZ", "哈萨克斯坦"),
    CZE("CZE", "CZ", "捷克"),
    SVK("SVK", "SK", "斯洛伐克"),
    NOR("NOR", "NO", "挪威"),
    SWE("SWE", "SE", "瑞典"),
    FIN("FIN", "FI", "芬兰"),
    PRT("PRT", "PT", "葡萄牙"),
    BLR("BLR", "BY", "白俄罗斯"),
    ROU("ROU", "RO", "罗马尼亚"),
    UKR("UKR", "UA", "乌克兰"),
    BRA("BRA", "BR", "巴西"),
    COL("COL", "CO", "哥伦比亚"),
    HUN("HUN", "HU", "匈牙利"),
    GEO("GEO", "GE", "格鲁吉亚"),
    LAT("LAT", "LV", "拉脱维亚"),
    LVA("LVA", "LV", "拉脱维亚"),
    AUT("AUT", "AT", "奥地利"),
    RSA("RSA", "ZA", "南非"),
    ZAF("ZAF", "ZA", "南非"),
    TPE("TPE", "TW", "中国台北"),
    TWN("TWN", "TW", "中国台湾"),
    THA("THA", "TH", "泰国"),
    IND("IND", "IN", "印度"),
    MEX("MEX", "MX", "墨西哥"),
    TUN("TUN", "TN", "突尼斯"),
    ISR("ISR", "IL", "以色列"),
    TUR("TUR", "TR", "土耳其"),
    EGY("EGY", "EG", "埃及"),
    MAR("MAR", "MA", "摩洛哥"),
    KOR("KOR", "KR", "韩国"),
    PRK("PRK", "KP", "朝鲜"),
    SGP("SGP", "SG", "新加坡"),
    MYS("MYS", "MY", "马来西亚"),
    IDN("IDN", "ID", "印度尼西亚"),
    PHL("PHL", "PH", "菲律宾"),
    VNM("VNM", "VN", "越南"),
    NZL("NZL", "NZ", "新西兰"),
    PAK("PAK", "PK", "巴基斯坦"),
    UZB("UZB", "UZ", "乌兹别克斯坦"),
    EST("EST", "EE", "爱沙尼亚"),
    LTU("LTU", "LT", "立陶宛"),
    SVN("SVN", "SI", "斯洛文尼亚"),
    SLO("SLO", "SI", "斯洛文尼亚"),
    BIH("BIH", "BA", "波黑"),
    MKD("MKD", "MK", "北马其顿"),
    MNE("MNE", "ME", "黑山"),
    CYP("CYP", "CY", "塞浦路斯"),
    MLT("MLT", "MT", "马耳他"),
    ALB("ALB", "AL", "阿尔巴尼亚"),
    MDA("MDA", "MD", "摩尔多瓦"),
    AZE("AZE", "AZ", "阿塞拜疆"),
    ARM("ARM", "AM", "亚美尼亚"),
    PER("PER", "PE", "秘鲁"),
    ECU("ECU", "EC", "厄瓜多尔"),
    URY("URY", "UY", "乌拉圭"),
    VEN("VEN", "VE", "委内瑞拉"),
    PRY("PRY", "PY", "巴拉圭"),
    BOL("BOL", "BO", "玻利维亚"),
    ISL("ISL", "IS", "冰岛"),
    IRL("IRL", "IE", "爱尔兰"),
    LUX("LUX", "LU", "卢森堡"),
    MON("MON", "MC", "摩纳哥"),
    MCO("MCO", "MC", "摩纳哥"),
    LIE("LIE", "LI", "列支敦士登");

    private final String code;
    private final String iso2;
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
                country.setFlagCode(e.iso2);
                return country;
            }
        }
        CountryVO country = new CountryVO();
        country.setCode(code);
        country.setName(code);
        country.setFlagCode(null);
        return country;
    }
}
