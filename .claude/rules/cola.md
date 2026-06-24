## COLA 架构分层

项目严格遵循 COLA 5.0 架构，
- 模块依赖方向：
    - `start（聚合启动，@SpringBootApplication 扫包 com.rally） → rally-adapter → rally-app`
    - `rally-app → rally-infrastructure`
    - `rally-infrastructure → rally-domain`
- `rally-adapter`:REST Controller（`com.rally.web`）+ 定时 Job（`com.rally.job`）
- `rally-app`: 应用层，编排领域层业务流程，不含领域能力。
- `rally-domain`:领域层，领域业务核心规则抽象（通过service和聚合根向外暴露）。通过gateway访问infrastructure。gateway存在两种操作中间件的结尾为Repository，访问三方的结尾为client，不要以gateway结尾。
- `rally-infrastructure`:Gateway 实现操作数据库、MyBatis-Plus ORM、外部 API Client、FastJson 配置。 gateway的实现类操作中间件的结尾为Repository，访问三方的结尾为client，不要以gateway结尾。
- `start`: Spring Boot 入口，`@MapperScan("com.rally.db")` |
