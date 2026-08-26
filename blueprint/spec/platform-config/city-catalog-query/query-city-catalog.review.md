# platform-config.city-catalog-query.flow.query-city-catalog 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 接口是否允许匿名访问，用户身份是否影响城市结果？
  > GET /city 未标 OptionalAuth；缺失或格式错误的 Authorization 返回 TOKEN_EXPIRED，无效 Bearer 令牌返回 TOKEN_INVALID。登录用户身份不参与城市列表构造。
  → 接口契约、详细流程第 1 步及鉴权异常分支

- [Q2] 城市名录从何加载、何时加载，读取失败如何表现？
  > ResourceConfigLoader 在 CityConfig 组件初始化时从 classpath city.json 读取并建立内存 Map；读取或 JSON 解析异常被捕获并加载为空名录，后续查询成功返回空数组。
  → 详细流程第 2、4 步及异常分支说明

- [Q3] 城市列表是否保持资源文件顺序，是否支持筛选、分页或排序？
  > 通过 HashMap values 生成列表，不保证资源文件或业务排序；接口无参数，不支持筛选、搜索、分页和排序。
  → 接口契约与详细流程第 3 步

- [Q4] 本查询是否只返回已开通城市，字段和城市数量口径是什么？
  > 返回完整可识别城市名录，不读取开通配置；当前 city.json 有 337 项，但数量随随包资源变化。每项仅 code、name、initials、pinyin。
  → 接口契约、详细流程第 4-5 步与服务边界
