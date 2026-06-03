## 数据库与 ORM
- 使用**MyBatis-Plus** 操作数据
- 放在 `rally-infrastructure/src/main/java/com/rally/db/{驼峰表名}`
- 总是创建对应的表的Mapper类和service Mapper 继承 `BaseMapper<PO>`，Service 继承 `ServiceImpl`， 对外暴露Repository 门面层封装 Service，供 app 层调用
- PO 实体用 `@TableName` 注解
- 无自定义 XML Mapper，查询总是用 `LambdaQueryWrapper`
