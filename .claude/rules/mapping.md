## 对象映射
- 两个对象转换不用手动set值， 统一使用 MapStruct（`org.mapstruct`） 
- MapStruct 用单例模式。 
  - 接口例：MatchAppConvertMapper INSTANCE = Mappers.getMapper(MatchAppConvertMapper.class);
  - 使用例： `MatchAppConvertMapper.INSTANCE.toPO`
- 当字段转换无逻辑时使： `@Mapping(target = "player1Id", source = "playerId")`,  
- 当字段有复杂转换逻辑时 `@Mapping(target = "matchIndex", expression = "java(extractMatchNumber(match.getMatchId()))")`， 并在底下实现对应方法`extractMatchNumber(match.getMatchId())`