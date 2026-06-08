## 对象映射
- 两个对象转换不用手动set值， 统一使用 MapStruct（`org.mapstruct`）
  - `private MeetupCardDTO toMeetupCardDTO(MeetupData data) {` 比如这种类型的代码转换。
- MapStruct 用单例模式。 
  - 在创建接口：MatchAppConvertMapper INSTANCE = Mappers.getMapper(MatchAppConvertMapper.class);
  - 在使用的时候，不要在使用类注册属性，而是直接调用类来访问实例： `MeetupPO meetupPO = MeetupConvertMapper.INSTANCE.toMeetupPO(data);`
- 当字段转换无逻辑时使： `@Mapping(target = "player1Id", source = "playerId")`,  
- 当字段有复杂转换逻辑时 `@Mapping(target = "matchIndex", expression = "java(extractMatchNumber(match.getMatchId()))")`， 并在底下实现对应方法`extractMatchNumber(match.getMatchId())`