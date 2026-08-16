package com.rally.tour.poster;

/**
 * 赛事特色素材库（可拔插，城市特色与中央球场特色共用一张表）。
 * <p>
 * 一个赛事只会命中一种特色，故合并为单一枚举，按级别决定查法（见 {@link TourLevelEnum}）：
 * <ul>
 *   <li>1000 以下（250 / 500）：中央球场无辨识度，用<b>举办城市</b>特色作远景点缀。
 *       用 city 匹配 {@link #fromCity}，desc 给可视化的<b>轻量地标关键词</b>（不报专有名词），避免地标塞满天际线抢占视角。</li>
 *   <li>1000 及以上（1000 / 大满贯 / 年终总决赛）：中央球场本身即标志性符号，突出<b>中央球场</b>建筑特色。
 *       用 tournamentId 匹配 {@link #fromTournamentId}，desc 给可视化的<b>建筑特征描述</b>（不报专有名词，只给可画出的特征，如「可开合顶棚、红土碗状环形看台」），避免生图模型认错球场。</li>
 * </ul>
 * code 为匹配键：低级别填城市名（小写），高级别填 tournamentId。
 */
public enum TourFeatureEnum {

    // 250 / 500：按举办城市匹配，特色只作远景点缀。
    CITY_BRISBANE("brisbane", "蜿蜒河湾、亚热带绿植与明亮的滨水现代天际线"),
    CITY_HONG_KONG("hong kong", "密集滨海高楼、层叠翠绿山体与穿行水面的渡船剪影"),
    CITY_AUCKLAND("auckland", "帆船点缀的海港、起伏火山丘与低密度滨水天际线"),
    CITY_ADELAIDE("adelaide", "林荫大道、浅色砂岩建筑与平缓开阔的城市轮廓"),
    CITY_MONTPELLIER("montpellier", "浅色石灰岩街区、纤细拱廊与地中海式梧桐树影"),
    CITY_ROTTERDAM("rotterdam", "棱角鲜明的现代高楼、港口吊臂与斜拉桥轮廓"),
    CITY_DALLAS("dallas", "开阔平原上的玻璃高楼群与几何感鲜明的城市剪影"),
    CITY_BUENOS_AIRES("buenos aires", "欧式浅色立面、宽阔林荫大道与紫色花树点缀"),
    CITY_DOHA("doha", "沙漠海湾、棕榈岸线与造型各异的未来感高楼群"),
    CITY_DELRAY_BEACH("delray beach", "棕榈成行的海滨街道、低矮彩色装饰风格建筑与碧蓝海面"),
    CITY_RIO_DE_JANEIRO("rio de janeiro", "花岗岩奇峰、弧形海湾与沿山海展开的白色城区"),
    CITY_DUBAI("dubai", "沙漠暖色地平线、棕榈绿洲与高低错落的未来感尖塔"),
    CITY_SANTIAGO("santiago", "密集现代城区背后横亘着高大连绵的雪山山脉"),
    CITY_ACAPULCO("acapulco", "新月形海湾、层叠热带山坡与沿岸延伸的灯火"),
    CITY_MARRAKECH("marrakech", "赭红城墙、方形宣礼塔剪影、棕榈与远处雪山"),
    CITY_HOUSTON("houston", "平坦辽阔地平线上的玻璃摩天楼群与宽阔城市绿带"),
    CITY_BUCHAREST("bucharest", "浅色古典宫殿立面、林荫大道与红瓦圆顶"),
    CITY_MUNICH("munich", "红瓦屋顶、洋葱形双塔与远处朦胧的雪山轮廓"),
    CITY_BARCELONA("barcelona", "规则街区、彩色曲面屋顶与成簇高耸的细长尖塔"),
    CITY_GENEVA("geneva", "宁静湖面、冲天水柱、浅色老城与远处雪峰"),
    CITY_HAMBURG("hamburg", "红砖仓库、水道桥梁与港口吊臂交错的滨水轮廓"),
    CITY_S_HERTOGENBOSCH("'s-hertogenbosch", "窄运河、砖砌阶梯山墙与高耸哥特式塔尖"),
    CITY_STUTTGART("stuttgart", "葡萄园山坡环抱的红瓦城区与纤细教堂尖塔"),
    CITY_LONDON("london", "宽阔河面、石砌钟楼与古典建筑交织的层次天际线"),
    CITY_HALLE("halle", "红瓦老城、成对石砌塔楼与河岸绿地"),
    CITY_MALLORCA("mallorca", "碧绿海湾、浅色石砌街区、棕榈与山地海岸"),
    CITY_EASTBOURNE("eastbourne", "白色海蚀峭壁、长直海滨步道与浅色维多利亚式建筑"),
    CITY_UMAG("umag", "湛蓝海湾、橙红屋顶与伸向海面的纤细钟塔"),
    CITY_GSTAAD("gstaad", "木质山间木屋、翠绿牧坡与层叠雪山"),
    CITY_BASTAD("bastad", "北欧海湾、低矮彩色木屋与柔和起伏的绿色山坡"),
    CITY_KITZBUHEL("kitzbuhel", "彩色中世纪街屋、木质山屋与陡峭雪山坡面"),
    CITY_ESTORIL("estoril", "大西洋海岸、棕榈步道与覆有彩色花砖的浅色别墅"),
    CITY_LOS_CABOS("los cabos", "仙人掌荒漠、金色岩山与伸入湛蓝海面的天然石拱"),
    CITY_WASHINGTON("washington", "宽阔轴线绿地、白色柱廊建筑与低矮圆顶轮廓"),
    CITY_WINSTON_SALEM("winston-salem", "红砖工业街区、林木覆盖的缓坡与纤细旧式塔楼"),
    CITY_HANGZHOU("hangzhou", "薄雾湖面、垂柳、石拱桥与远山间的层檐塔影"),
    CITY_CHENGDU("chengdu", "竹林薄雾、深灰层叠瓦檐与舒缓低矮的城市轮廓"),
    CITY_TOKYO("tokyo", "密集霓虹高楼、红白通信塔与传统曲面屋檐剪影"),
    CITY_BEIJING("beijing", "灰瓦重檐、朱红墙体与规整宽阔的现代城市轴线"),
    CITY_LYON("lyon", "两河交汇水面、赭色屋顶与山丘上的浅色双塔"),
    CITY_BRUSSELS("brussels", "金色雕饰立面、阶梯山墙与高耸细密的哥特式塔尖"),
    CITY_ALMATY("almaty", "浓密林荫大道与现代城区背后迫近的巨大雪山"),
    CITY_BASEL("basel", "碧绿河湾、彩色老城立面与红砂岩双塔"),
    CITY_VIENNA("vienna", "浅色帝国式宫殿、铜绿圆顶与优雅连续的古典街廓"),
    CITY_STOCKHOLM("stockholm", "岛屿水道、暖色窄楼与成组教堂尖塔"),
    CITY_PERTH_SYDNEY("perth + sydney", "明亮海港、白色帆状屋顶、钢拱桥与另一侧开阔的西海岸天际线"),
    CITY_HOBART("hobart", "帆船港湾、砂岩仓库与背后平顶状的深色山脊"),
    CITY_ABU_DHABI("abu dhabi", "白色圆顶、棕榈滨海大道与沙色调现代高楼"),
    CITY_OSTRAVA("ostrava", "红砖工业厂房、高炉钢架与粗犷现代城市轮廓"),
    CITY_CLUJ_NAPOCA("cluj-napoca", "红瓦老城、彩色巴洛克立面与山谷中的教堂尖塔"),
    CITY_AUSTIN("austin", "河岸现代高楼、绿色丘陵与跨水面的拱形桥梁"),
    CITY_MERIDA("mérida", "粉彩殖民街屋、白色石灰岩立面与热带棕榈"),
    CITY_CHARLESTON("charleston", "粉彩窄楼、成排棕榈与海湾上密集的教堂尖塔"),
    CITY_BOGOTA("bogota", "红砖密集城区紧贴陡峭翠绿山脊展开"),
    CITY_LINZ("linz", "宽阔河面、粉彩老城与通透现代玻璃建筑相接"),
    CITY_ROUEN("rouen", "密集半木结构街屋、灰蓝屋顶与极高的哥特式尖塔"),
    CITY_STRASBOURG("strasbourg", "运河边半木结构房屋、石桥与单座镂空尖塔"),
    CITY_RABAT("rabat", "白墙绿瓦、棕榈与方正赭色城塔组成的低矮天际线"),
    CITY_NOTTINGHAM("nottingham", "红砖屋顶、哥特式塔楼与大片深绿色林木"),
    CITY_BERLIN("berlin", "现代玻璃穹顶、古典柱门与纤细球形高塔剪影"),
    CITY_BAD_HOMBURG("bad homburg", "浅色温泉别墅、整齐花园与森林山坡间的塔楼"),
    CITY_IASI("iasi", "繁复新哥特式宫殿剪影、教堂圆顶与浓密林荫"),
    CITY_ATHENS("athens", "白色城区铺满丘陵，岩石高地上立着成排古典石柱"),
    CITY_PRAGUE("prague", "红瓦屋海、石拱桥与成簇尖塔圆顶"),
    CITY_MEMPHIS("memphis", "宽阔大河、钢桁架桥与低矮红砖音乐街区"),
    CITY_MONTERREY("monterrey", "现代高楼群被陡峭、鞍形轮廓鲜明的岩山紧密环抱"),
    CITY_GUADALAJARA("guadalajara", "浅色殖民广场、成对哥特式尖塔与紫色花树"),
    CITY_SAO_PAULO("sao paulo", "无边延伸的高密度楼群、屋顶天台与灰蓝城市薄雾"),
    CITY_SINGAPORE("singapore", "热带滨水高楼、树冠形巨构与层叠空中花园"),
    CITY_SEOUL("seoul", "密集现代高楼、传统深色瓦檐与环城的森林山体"),
    CITY_NINGBO("ningbo", "河流交汇处的现代高楼、弧形桥梁与港城水岸"),
    CITY_OSAKA("osaka", "密集霓虹楼群、层叠白墙绿瓦城楼与高架交通线"),
    CITY_GUANGZHOU("guangzhou", "宽阔江面、彩灯桥梁与纤细扭转的高塔"),
    CITY_CHENNAI("chennai", "热带海岸、成排棕榈与布满彩色雕塑的层级塔门"),

    // 1000 / GS / Finals：按 tournamentId 匹配，突出中央球场建筑本身。
    AUSTRALIAN_OPEN_580("580", "四层深蓝看台围成近圆形碗体，白色拱形可开合顶棚覆盖蓝色硬地"),
    INDIAN_WELLS_404("404", "露天巨型椭圆碗状看台层层下沉，棕榈与荒漠山脉从看台上缘露出"),
    MIAMI_403("403", "硬地球场嵌入巨型橄榄球场内部，四层看台与宽阔白色环形顶棚围合"),
    MONTE_CARLO_410("410", "红土球场沿山坡层叠嵌入，浅色露天看台外可见海湾与陡峭岩壁"),
    MADRID_1536("1536", "银灰色矩形金属场馆，三片平行的可开合顶棚悬在红土看台上方"),
    ROME_416("416", "露天红土球场被白色椭圆外墙和陡峭环形看台包围，顶部环绕细长棚架"),
    FRENCH_OPEN_520("520", "红土球场置于方正深碗看台，十一片翼状白色可开合顶棚横跨上空"),
    WIMBLEDON_540("540", "草地球场被深绿色看台围合，半透明织物可开合顶棚与砖墙绿植相接"),
    CANADA_MONTREAL_421("421", "露天蓝色硬地位于高耸多层看台中，开放转角与白色钢构棚架形成层次"),
    CINCINNATI_422("422", "蓝色硬地被多层灰蓝看台围合，宽大白色弧形遮阳棚悬在上层座席上方"),
    US_OPEN_560("560", "巨型陡峭碗状看台上方竖立粗壮钢柱与网格梁架，两片可开合顶棚覆盖蓝色硬地"),
    SHANGHAI_5014("5014", "八片白色花瓣状钢结构顶棚可旋转开合，环抱蓝紫色硬地与圆形看台"),
    PARIS_352("352", "封闭式巨型室内场馆，黑色陡峭看台与高挑穹顶围出剧场般的中央硬地"),
    AUSTRALIAN_OPEN_901("901", "四层深蓝看台围成近圆形碗体，白色拱形可开合顶棚覆盖蓝色硬地"),
    DOHA_1003("1003", "露天蓝色硬地被浅灰阶梯式看台包围，主看台顶端横跨白色弧形遮阳篷"),
    DUBAI_718("718", "紧凑露天蓝色硬地四周环绕连续看台，主看台上方覆盖弧形白色悬挑棚"),
    INDIAN_WELLS_609("609", "露天巨型椭圆碗状看台层层下沉，棕榈与荒漠山脉从看台上缘露出"),
    MIAMI_902("902", "硬地球场嵌入巨型橄榄球场内部，四层看台与宽阔白色环形顶棚围合"),
    MADRID_1038("1038", "银灰色矩形金属场馆，三片平行的可开合顶棚悬在红土看台上方"),
    ROME_709("709", "露天红土球场被白色椭圆外墙和陡峭环形看台包围，顶部环绕细长棚架"),
    FRENCH_OPEN_903("903", "红土球场置于方正深碗看台，十一片翼状白色可开合顶棚横跨上空"),
    WIMBLEDON_904("904", "草地球场被深绿色看台围合，半透明织物可开合顶棚与砖墙绿植相接"),
    CANADA_TORONTO_806("806", "露天蓝色硬地坐落于矩形深碗看台中，两侧高耸、端线方向较低并向天空敞开"),
    CINCINNATI_1017("1017", "蓝色硬地被多层灰蓝看台围合，宽大白色弧形遮阳棚悬在上层座席上方"),
    US_OPEN_905("905", "巨型陡峭碗状看台上方竖立粗壮钢柱与网格梁架，两片可开合顶棚覆盖蓝色硬地"),
    BEIJING_1020("1020", "菱形外轮廓的钢构场馆包围蓝色硬地，两片可开合顶棚在中央形成长条天窗"),
    WUHAN_1075("1075", "露天蓝紫色硬地被双层椭圆看台环抱，银灰外墙与开放式顶缘勾勒流线轮廓"),
    WTA_FINALS_RIYADH_808("808", "封闭式室内硬地被黑色环形看台包围，深色穹顶下多束聚光灯汇聚场心");

    private final String code;
    private final String desc;

    TourFeatureEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 城市特色查找（1000 以下用）。未命中返回 null。 */
    public static TourFeatureEnum fromCity(String city) {
        return match(city == null ? null : city.trim().toLowerCase());
    }

    /** 中央球场特色查找（1000 及以上用）。未命中返回 null。 */
    public static TourFeatureEnum fromTournamentId(String tournamentId) {
        return match(tournamentId == null ? null : tournamentId.trim());
    }

    private static TourFeatureEnum match(String key) {
        if (key == null || key.isBlank()) return null;
        for (TourFeatureEnum feature : values()) {
            if (feature.code.equals(key)) return feature;
        }
        return null;
    }
}
