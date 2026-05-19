# Match Parser 重构设计

**日期**: 2026-05-19  
**范围**: `rally-app` 中的 Tennis 采集体系  
**目标**: 通过 `MatchParser<R,S>` 抽象类 + `MatchCollectManager` 编排层，将各三方数据源的解析逻辑从 Collect Services 中分离出来，消除按 ATP/WTA 分支散落各处的重复逻辑。

---

## 背景

当前 `TennisCollectService` 通过 `parseAtpDraw()` / `parseWtaDraw()` 等方法分别调用四个 Collect Services，每个 Service 内部各自维护 ATP 和 WTA 的解析逻辑。新增数据源或调整解析规则需要改动多处。

---

## 核心抽象：`MatchParser<R, S>`

```
R = 三方接口原始响应类型（如 AtpDrawsResponse）
S = 单个签表的切片类型（如 AtpDrawsResponse.DrawSection）
```

```java
public abstract class MatchParser<R, S> {

    /** 从原始响应中拆分出多个签表切片（一次响应可能含单打+双打） */
    public abstract List<DrawResult<S>> getDraws(R response);

    /** 判断该切片的赛制（SINGLES / DOUBLES / MIXED） */
    public abstract Discipline parseDiscipline(S slice);

    /** 从切片中提取 tournamentId，供 Manager 做 tournament 存在性校验 */
    public abstract String getTournamentId(S slice);

    /** 解析签表元信息（drawSize、totalRounds） */
    public abstract DrawMeta getDrawMeta(S slice);

    /** 从切片提取所有比赛（含 tournamentId 和 drawId 注入） */
    public abstract List<Match> getMatches(DrawResult<S> draw, String tournamentId, Long drawId);

    /** 从切片提取所有参赛球员 */
    public abstract List<Player> getPlayers(DrawResult<S> draw);

    /** 从切片提取参赛资格 / 种子信息 */
    public abstract List<TournamentEntry> getEntries(DrawResult<S> draw, Long drawId);
}
```

**`DrawResult<S>`**（切片包装器）：

```java
public class DrawResult<S> {
    private final S slice;          // 强类型原始切片
    private final Discipline discipline;
    private final DrawMeta meta;    // drawSize / totalRounds
}
```

**`DrawMeta`**（值对象）：

```java
public class DrawMeta {
    private final int drawSize;
    private final int totalRounds;
}
```

`parseDiscipline()` 在 `getDraws()` 内部调用并存入 `DrawResult`，Manager 直接读 `draw.getDiscipline()`，无需额外调用。

---

## 实现类清单（以调用接口为维度）

| 实现类 | 数据源接口 | R | S |
|--------|-----------|---|---|
| `AtpDrawMatchParser` | TennisTv 签表接口 | `AtpDrawsResponse` | `AtpDrawsResponse.DrawSection`（MS/MD 等各 discipline 段） |
| `WtaDrawMatchParser` | WTA 签表接口 | `WtaDrawsResponse` | `WtaDrawsResponse.DrawData` |
| `AtpOopMatchParser` | TennisTv OOP 接口 | `List<AtpOopResponse>` | `AtpOopResponse`（按赛事分组） |

**单双打区分规则**：
- `AtpDrawMatchParser`：ATP 响应的 MS / MD 字段对应不同 discipline，可通过字段名直接确定
- `WtaDrawMatchParser`：WTA 响应中部分接口含明确字段，部分需从 drawName 或参赛者数量推断
- `AtpOopMatchParser`：通过比赛 discipline 字段读取

**Live 比赛**（`atpFromLive()` / `wtaFromLive()`）：当前逻辑为更新现有比赛进度，不涉及 draw 创建，本次暂不纳入 Parser 体系，保持现状。后续可单独引入 `LiveMatchParser` 变体。

---

## `MatchCollectManager`

位于 `rally-app/src/main/java/com/rally/tennis/MatchCollectManager.java`。

```java
@Service
public class MatchCollectManager {

    /** 是否采集双打（通过配置或构造注入控制） */
    @Value("${tennis.collect.doubles:false}")
    private boolean collectDoubles;

    @Resource private TournamentCollectService tournamentCollectService;
    @Resource private DrawCollectService drawCollectService;
    @Resource private MatchCollectService matchCollectService;
    @Resource private PlayerCollectService playerCollectService;

    /**
     * Draw 型采集流程：创建 draw → 保存比赛 → 保存球员 → 保存参赛资格
     */
    public <R, S> void collectFromDraw(R response, MatchParser<R, S> parser) {
        List<DrawResult<S>> draws = parser.getDraws(response);

        for (DrawResult<S> draw : draws) {
            // 1. Discipline 过滤
            if (!shouldCollect(draw.getDiscipline())) continue;

            String tournamentId = parser.getTournamentId(draw.getSlice());

            // 2. Tournament 存在性校验
            if (!tournamentCollectService.exists(tournamentId)) {
                log.warn("Tournament not found, skip draw: {}", tournamentId);
                continue;
            }

            // 3. 创建/更新 Draw 记录
            DrawMeta meta = draw.getMeta();
            Long drawId = drawCollectService.saveOrUpdate(
                tournamentId, draw.getDiscipline(), meta.getDrawSize(), meta.getTotalRounds()
            );

            // 4. 保存比赛
            matchCollectService.saveMatches(parser.getMatches(draw, tournamentId, drawId));

            // 5. 保存球员
            playerCollectService.savePlayers(parser.getPlayers(draw));

            // 6. 保存参赛资格/种子
            tournamentCollectService.saveEntries(parser.getEntries(draw, drawId));
        }
    }

    private boolean shouldCollect(Discipline discipline) {
        return discipline != Discipline.DOUBLES || collectDoubles;
    }
}
```

**Manager 与 TennisCollectService 的边界**：Manager 只负责流程编排，Client 调用仍在 `TennisCollectService`，存储操作仍在各 Collect Service，Parser 负责纯解析。

---

## TennisCollectService 变化

```java
// 重构前（分散在四个 Collect Service 里）
private void parseAtpDraw(String tournamentId, int year) {
    AtpDrawsResponse response = tennisTvClient.getDraws(tournamentId, year);
    if (response != null && response.getMS() != null && ...) {
        Long drawId = drawCollectService.atp(response, tournamentId, year);
        matchCollectService.atpFromDraw(response, tournamentId, drawId, year);
        tournamentCollectService.atpTournamentEntry(response, drawId);
        playerCollectService.atpFromDraw(response);
    }
}

// 重构后（委托给 Manager）
private void parseAtpDraw(String tournamentId, int year) {
    AtpDrawsResponse response = tennisTvClient.getDraws(tournamentId, year);
    if (response != null) {
        matchCollectManager.collectFromDraw(response, atpDrawMatchParser);
    }
}
```

`currentDraws()` / `liveMatch()` / `oop()` 的对外接口不变。

---

## 各 Collect Service 的职责变化

| Service | 重构后职责 |
|---------|-----------|
| `DrawCollectService` | 保留 `saveOrUpdate(tournamentId, discipline, drawSize, totalRounds)` |
| `MatchCollectService` | 保留 `saveMatches(List<Match>)` / `updateMatches(List<Match>)` |
| `PlayerCollectService` | 保留 `savePlayers(List<Player>)` |
| `TournamentCollectService` | 新增 `exists(tournamentId)`；新增 `saveEntries(List<TournamentEntry>)` |

各 Service 内部原有 ATP/WTA 特定的 `atp()` / `wta()` 解析方法在迁移完成后删除。

---

## 包结构

```
rally-app/src/main/java/com/rally/tennis/
├── parser/
│   ├── MatchParser.java          # 抽象类 <R, S>
│   ├── DrawResult.java
│   ├── DrawMeta.java
│   ├── AtpDrawMatchParser.java
│   ├── WtaDrawMatchParser.java
│   └── AtpOopMatchParser.java
├── MatchCollectManager.java      # 新增编排层
├── TennisCollectService.java     # 精简，只做 Client 调用 + 委托
├── DrawCollectService.java
├── MatchCollectService.java
├── PlayerCollectService.java
└── TournamentCollectService.java
```

---

## 迁移策略

1. **新增** `parser/` 包和三个 Parser 实现类，不改动现有代码
2. **新增** `MatchCollectManager`，注入 Parser 和 Collect Services
3. **逐一迁移** `parseAtpDraw()` → Manager，验证功能等价后再迁移 WTA
4. **迁移** OOP 流程（`atpOopMatchParser`）
5. **删除** 各 Collect Service 内已迁移的 ATP/WTA 特定方法

每步均可独立测试，无需一次性切换。
