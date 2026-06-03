## 对象映射

- 对象映射使用 MapStruct（`org.mapstruct`） 的单例模式:`MatchAppConvertMapper INSTANCE = Mappers.getMapper(MatchAppConvertMapper.class);`
- 当只需要原类型的单个的属性的时候使用named注解: `@Named("groupToId")`