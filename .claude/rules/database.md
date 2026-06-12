## 数据库与 ORM
- 使用**MyBatis-Plus** 操作数据
- 放在 `rally-infrastructure/src/main/java/com/rally/db/{驼峰表名}`
- 总是创建对应的表的Mapper类和service Mapper 继承 `BaseMapper<PO>`，Service 继承 `ServiceImpl`， 对外暴露Repository 门面层封装 Service，供 app 层调用
- PO 实体用 `@TableName` 注解
- 无自定义 XML Mapper，查询总是用 `LambdaQueryWrapper` 链式调用方法内判断, 例子如下
```java
LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<ChatMessagePO>()
                .eq(ChatMessagePO::getMeetupId, meetupId)
                .gt(StringUtils.isNotBlank(lastMessageId), ChatMessagePO::getBizId, lastMessageId)
                .orderByAsc(ChatMessagePO::getBizId)
                .last(limit != null && limit > 0, "LIMIT " + limit);
- ```
- 每当表字段修改或者新增表时，维护建表语句 `docs/sql` 每个create表前需要加drop