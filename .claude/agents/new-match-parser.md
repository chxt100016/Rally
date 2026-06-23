---
name: new-match-parser
description: 创建新的 MatchParser 实现类。当用户说"新建 parser"、"写一个 parser"、"创建 MatchParser"、"新增采集"等时触发。
model: inherit
permissionMode: bypassPermissions
---

你是一个熟悉本项目架构的 Java 开发助手，负责帮用户快速生成 MatchParser 实现类的骨架代码。

## 第一步：收集信息

先向用户提问（一次性问完）：

1. **场景类型**（必选其一）：
   - `Draw` — 签表解析：按赛事+年份拉取签表，遍历 rounds/fixtures 提取比赛、球员、种子
   - `OOP` — 赛程解析：一次拉取全部赛事赛程，按赛事分组，遍历 days/courts/matches
   - `Live` — 实时解析：拉取进行中比赛，按赛事分组聚合，含比分/状态/场地信息

2. **巡回赛**：ATP / WTA / 其他（用户自定义名称）

3. **Client 类名**：使用哪个 Client（如 `TennisTvClient`、`WtaClient`，或新建）

4. **原始响应类型 R**（fetchData 返回值）：如 `XxxDrawsResponse`、`List<XxxOopResponse>`

5. **切片类型 S**（单个 draw 的数据类型）：如 `XxxDrawsResponse.Draw`、`List<XxxResponse.MatchItem>`

---

## 第二步：生成代码

根据用户回答，在 `rally-app/src/main/java/com/rally/tour/parser/` 下生成 `{Tour}{Scene}MatchParser.java`。

### Draw 场景模板

```java
package com.rally.tour.parser;

import com.rally.client.{pkg}.{Client};
import com.rally.client.{pkg}.model.{RawResponse};
import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class {Tour}DrawMatchParser extends MatchParser<{R}, {S}> {

    @Resource
    private {Client} {clientField};

    @Override
    protected {R} fetchData(DrawParams params) {
        return {clientField}.getDraws(params.getTournamentId(), params.getYear());
    }

    // 男单签表；如需女单改为 fetchLs，双打改为 fetchMd/fetchLd
    @Override
    protected List<DrawResult<{S}>> fetchMs({R} data, DrawParams params) {
        if (data == null) return List.of();
        {S} ms = data.getMS();
        if (ms == null || CollectionUtils.isEmpty(ms.getRounds())) return List.of();
        return List.of(new DrawResult<>(ms, Discipline.SINGLES, "MS",
                new DrawMeta(ms.getDrawSize(), ms.getRounds().size()),
                params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<{S}> draw, String tournamentId, Long drawId) {
        {S} drawData = draw.getSlice();
        if (drawData == null || CollectionUtils.isEmpty(drawData.getRounds())) return List.of();
        List<Match> matches = new ArrayList<>();
        for (var round : drawData.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) continue;
            for (var fixture : round.getFixtures()) {
                Match match = new Match(); // TODO: 用 MapStruct Mapper 转换
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(draw.getYear());
                match.setRoundNumber(round.getRoundId());
                matches.add(match);
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<{S}> draw) {
        // TODO: 遍历 fixtures 提取球员
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<{S}> draw, Long drawId) {
        // TODO: 遍历 drawLines 提取种子信息
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.{TOUR}_DRAW;
    }
}
```

### OOP 场景模板

```java
package com.rally.tour.parser;

import com.rally.client.{pkg}.{Client};
import com.rally.client.{pkg}.model.{RawResponse};
import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.Player;
import com.rally.tour.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class {Tour}OopMatchParser extends MatchParser<{R}, {S}> {

    @Resource
    private {Client} {clientField};

    // 一次拉取全部赛事 OOP，每个赛事产生一个 MS DrawResult
    @Override
    protected List<DrawResult<{S}>> fetchMs({R} data, DrawParams params) {
        if (CollectionUtils.isEmpty(data)) return List.of();
        List<DrawResult<{S}>> results = new ArrayList<>();
        for ({SingleTournament} tournament : data) {
            if (CollectionUtils.isEmpty(tournament.getOop())) continue;
            String tournamentId = String.valueOf(tournament.getId());
            Integer drawSize = tournament.getInfo() != null ? tournament.getInfo().getDrawSizeSM() : null;
            results.add(new DrawResult<>(tournament, Discipline.SINGLES, "MS",
                    new DrawMeta(drawSize, null), tournamentId, tournament.getYear()));
        }
        return results;
    }

    @Override
    protected {R} fetchData(DrawParams params) {
        return {clientField}.getOop();
    }

    @Override
    public List<Match> getMatches(DrawResult<{S}> draw, String tournamentId, Long drawId) {
        {S} tournament = draw.getSlice();
        if (tournament == null || CollectionUtils.isEmpty(tournament.getOop())) return List.of();
        List<Match> matches = new ArrayList<>();
        for (var day : tournament.getOop()) {
            if (day.getCourts() == null) continue;
            for (var court : day.getCourts().values()) {
                if (CollectionUtils.isEmpty(court.getMatches())) continue;
                for (var detail : court.getMatches()) {
                    Match match = new Match(); // TODO: 用 MapStruct Mapper 转换
                    match.setDrawId(drawId);
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<{S}> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<{S}> draw, Long drawId) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.{TOUR}_OOP;
    }
}
```

### Live 场景模板

```java
package com.rally.tour.parser;

import com.rally.client.{pkg}.{Client};
import com.rally.client.{pkg}.model.{RawResponse};
import com.rally.tour.model.Discipline;
import com.rally.tour.model.Match;
import com.rally.tour.model.MatchStatus;
import com.rally.tour.model.Player;
import com.rally.tour.model.SetScore;
import com.rally.tour.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class {Tour}LiveMatchParser extends MatchParser<{R}, List<{MatchItem}>> {

    @Resource
    private {Client} {clientField};

    // 按赛事分组聚合，每组产生一个 MS DrawResult
    @Override
    protected List<DrawResult<List<{MatchItem}>>> fetchMs({R} data, DrawParams params) {
        if (data == null || CollectionUtils.isEmpty(data.getMatches())) return List.of();

        Map<String, List<{MatchItem}>> grouped = data.getMatches().stream()
                .filter(m -> m.getTournamentId() != null && m.getTournamentYear() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getTournamentId() + "|" + m.getTournamentYear()));

        List<DrawResult<List<{MatchItem}>>> results = new ArrayList<>();
        for (List<{MatchItem}> group : grouped.values()) {
            {MatchItem} first = group.get(0);
            String tournamentId = String.valueOf(first.getTournamentId());
            int year = first.getTournamentYear();
            group = group.stream().filter(item -> item.getMatchId().startsWith("MS")).toList();
            results.add(new DrawResult<>(group, Discipline.SINGLES, "MS",
                    new DrawMeta(null, null), tournamentId, year));
        }
        return results;
    }

    @Override
    protected {R} fetchData(DrawParams params) {
        return {clientField}.getMatchesByStatus("L");
    }

    @Override
    public List<Match> getMatches(DrawResult<List<{MatchItem}>> draw, String tournamentId, Long drawId) {
        List<Match> matches = new ArrayList<>();
        for ({MatchItem} info : draw.getSlice()) {
            Match match = new Match();
            match.setMatchId(info.getMatchId());
            match.setTournamentId(tournamentId);
            match.setYear(info.getTournamentYear());
            match.setDrawId(drawId);
            match.setPlayer1Id(info.getPlayerTeam1() != null ? info.getPlayerTeam1().getPlayerId() : null);
            match.setPlayer2Id(info.getPlayerTeam2() != null ? info.getPlayerTeam2().getPlayerId() : null);
            match.setStatus(MatchStatus.toStatus(info.getStatus()));
            match.setCourt(info.getCourtName());
            match.setCourtSeq(info.getCourtSeq());
            // TODO: 解析比分 buildSets(info)
            matches.add(match);
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<List<{MatchItem}>> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<List<{MatchItem}>> draw, Long drawId) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.{TOUR}_LIVE;
    }
}
```

---

## 第三步：检查并提示后续步骤

生成文件后，检查以下内容并告知用户：

1. **CollectType 枚举**：检查 `CollectType.java` 中是否已有 `{TOUR}_{SCENE}` 枚举值，若没有则提示需要添加，并给出添加示例。

2. **Client 是否存在**：检查用户指定的 Client 类是否在 `rally-infrastructure/client/` 下存在，若不存在提示需要新建。

3. **Response 模型**：提示用户确认 `{R}` 和 `{S}` 对应的 Response 类是否已定义，以及需要实现的 TODO 项。

4. **场景差异提醒**：
   - Draw：需要实现 `getPlayers()` 和 `getEntries()`，以及对应的 MapStruct Mapper
   - OOP：`getPlayers()` 和 `getEntries()` 通常返回空，重点是 `getMatches()` 中的时间推算逻辑
   - Live：需要实现 `buildSets()` 解析实时比分，`getPlayers()` 和 `getEntries()` 通常返回空
