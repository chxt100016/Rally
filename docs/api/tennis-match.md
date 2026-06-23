# 网球赛事接口文档

**Base URL**: `/api/rally/tennis/match`

---

## 1. 即将开始的比赛

**GET** `/upcoming`

获取指定赛事中状态为"即将开始"的比赛，按日期分组返回。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentIds` | `string[]` | 是 | 赛事 ID 列表，逗号分隔或多次传参 |

**示例**
```
GET /api/rally/tennis/match/upcoming?tournamentIds=t001&tournamentIds=t002
```

---

## 2. 已结束的比赛

**GET** `/finished`

获取指定赛事中状态为"已结束"的比赛，按轮次分组返回。

**请求参数**

| 参数名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tournamentIds` | `string[]` | 是 | 赛事 ID 列表 |

**示例**
```
GET /api/rally/tennis/match/finished?tournamentIds=t001&tournamentIds=t002
```

---

## 通用响应结构

两个接口响应结构相同：

```json
{
  "code": 0,
  "message": null,
  "data": {
    "seed": [...],
    "match": [...]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `number` | `0` 表示成功，非 0 表示业务错误 |
| `message` | `string\|null` | 错误信息，成功时为 null |
| `data.seed` | `SeedGroupDTO[]` | 种子球员分组列表 |
| `data.match` | `MatchGroupDTO[]` | 比赛分组列表 |

---

## 数据结构

### `SeedGroupDTO` — 种子球员分组

| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | `string` | 分组类型（如赛事类型：ATP / WTA） |
| `data` | `SeedVO[]` | 种子球员列表 |

### `SeedVO` — 种子球员

| 字段 | 类型 | 说明 |
|---|---|---|
| `playerId` | `string` | 球员 ID |
| `name` | `string` | 球员姓名（中文） |
| `seed` | `number` | 种子号 |
| `status` | `string` | `ACTIVE`（在赛） / `ELIMINATED`（已淘汰） |
| `label` | `string\|null` | 淘汰轮次中文名，如 `"8强"`；仅 `ELIMINATED` 时有值 |
| `tour` | `string` | 赛事类型，如 `ATP` / `WTA` |
| `tournamentId` | `string` | 所属赛事 ID |
| `country` | `CountryVO` | 所属国家 |

### `MatchGroupDTO` — 比赛分组（支持嵌套）

| 字段 | 类型 | 说明 |
|---|---|---|
| `key` | `string` | 分组原始值：日期为 `yyyy-MM-dd`，球场为球场名，轮次为枚举值 |
| `name` | `string` | 分组展示名：日期为 `今天`/`明天`/具体日期，轮次为中文，如 `"4强"` |
| `data` | `MatchQueryVO[]` | 该分组下的比赛列表 |
| `children` | `MatchGroupDTO[]\|null` | 子分组，用于多级分组展示 |

### `MatchQueryVO` — 比赛详情

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `string` | 比赛 ID |
| `tournamentId` | `string` | 所属赛事 ID |
| `court` | `string` | 球场名称 |
| `courtSeq` | `number` | 球场序号（用于排序） |
| `round` | `string` | 轮次枚举值 |
| `roundShow` | `string` | 轮次中文展示名，如 `"4强"` |
| `status` | `string` | 比赛状态原始值 |
| `statusLabel` | `string` | 比赛状态中文展示，如 `"进行中"` |
| `scheduledShow` | `string` | 预定开赛时间展示文本 |
| `date` | `string` | 比赛日期，格式 `yyyy-MM-dd` |
| `startedAt` | `string\|null` | 实际开始时间，ISO 8601，如 `2026-06-23T14:00:00` |
| `scheduledAt` | `string\|null` | 预定开始时间，ISO 8601 |
| `player1` | `PlayerVO` | 球员1 |
| `player2` | `PlayerVO` | 球员2 |
| `sets` | `SetScoreVO[]` | 各盘比分列表 |
| `currentSet` | `number\|null` | 当前局数（进行中时有值） |
| `currentSetScore` | `string\|null` | 当前局分，如 `"40-30"` |
| `winnerId` | `string\|null` | 胜者球员 ID，比赛结束后有值 |
| `durationShow` | `string\|null` | 比赛时长展示文本，如 `"1h 23m"` |

### `PlayerVO` — 球员

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `string` | 球员 ID |
| `name` | `string` | 球员姓名（中文） |
| `seed` | `number\|null` | 种子号，无种子时为 null |
| `country` | `CountryVO` | 所属国家 |

### `SetScoreVO` — 单盘比分

| 字段 | 类型 | 说明 |
|---|---|---|
| `number` | `number` | 盘序，从 1 开始 |
| `player1` | `number` | 球员1 得局数 |
| `player2` | `number` | 球员2 得局数 |
| `tiebreak1` | `number\|null` | 球员1 抢七比分，非抢七盘为 null |
| `tiebreak2` | `number\|null` | 球员2 抢七比分，非抢七盘为 null |

### `CountryVO` — 国家

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `string` | 国家代码，如 `"CHN"` |
| `name` | `string` | 国家名称 |

---

## 响应示例

### `/upcoming` 响应示例

```json
{
  "code": 0,
  "data": {
    "match": [
      {
        "children": [
          {
            "data": [
              {
                "court": "Centre Court",
                "date": "2026-06-23",
                "id": "MS022",
                "player1": {
                  "country": {
                    "code": "ARG",
                    "name": "阿根廷"
                  },
                  "id": "B0FV",
                  "name": "Burruchaga"
                },
                "player2": {
                  "country": {
                    "code": "GBR",
                    "name": "英国"
                  },
                  "id": "F0DM",
                  "name": "Fery"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledAt": "2026-06-23 21:30:00",
                "scheduledShow": "不早于21:30",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              },
              {
                "court": "Centre Court",
                "date": "2026-06-23",
                "id": "MS028",
                "player1": {
                  "country": {
                    "code": "FRA",
                    "name": "法国"
                  },
                  "id": "HH26",
                  "name": "Humbert",
                  "seed": 6
                },
                "player2": {
                  "country": {
                    "code": "ITA",
                    "name": "意大利"
                  },
                  "id": "B0GG",
                  "name": "Bellucci"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledAt": "2026-06-23 22:40:00",
                "scheduledShow": "随后",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              }
            ],
            "key": "Centre Court",
            "name": "Centre Court"
          },
          {
            "data": [
              {
                "court": "Court 1",
                "date": "2026-06-23",
                "id": "MS021",
                "player1": {
                  "country": {
                    "code": "ARG",
                    "name": "阿根廷"
                  },
                  "id": "T0A1",
                  "name": "Tirante"
                },
                "player2": {
                  "country": {
                    "code": "GBR",
                    "name": "英国"
                  },
                  "id": "F0BY",
                  "name": "Fearnley"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledAt": "2026-06-23 18:00:00",
                "scheduledShow": "18:00开赛",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              },
              {
                "court": "Court 1",
                "date": "2026-06-23",
                "id": "MS030",
                "player1": {
                  "country": {
                    "code": "ITA",
                    "name": "意大利"
                  },
                  "id": "A0FC",
                  "name": "Arnaldi"
                },
                "player2": {
                  "country": {
                    "code": "GBR",
                    "name": "英国"
                  },
                  "id": "H0DC",
                  "name": "Hussey"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledAt": "2026-06-23 19:10:00",
                "scheduledShow": "随后",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              }
            ],
            "key": "Court 1",
            "name": "Court 1"
          },
          {
            "data": [
              {
                "court": "Court 12",
                "date": "2026-06-23",
                "id": "MS029",
                "player1": {
                  "country": {
                    "code": "USA",
                    "name": "美国"
                  },
                  "id": "B0CD",
                  "name": "Brooksby"
                },
                "player2": {
                  "country": {
                    "code": "AUS",
                    "name": "澳大利亚"
                  },
                  "id": "V832",
                  "name": "Vukic"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledAt": "2026-06-23 18:00:00",
                "scheduledShow": "18:00开赛",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              }
            ],
            "key": "Court 12",
            "name": "Court 12"
          },
          {
            "data": [
              {
                "court": "Court 4",
                "date": "2026-06-23",
                "id": "MS018",
                "player1": {
                  "country": {
                    "code": "GER",
                    "name": "德国"
                  },
                  "id": "AE14",
                  "name": "Altmaier"
                },
                "player2": {
                  "country": {
                    "code": "USA",
                    "name": "美国"
                  },
                  "id": "K0AZ",
                  "name": "Kovacevic"
                },
                "round": "R32",
                "roundShow": "32强",
                "scheduledShow": "随后",
                "sets": [],
                "status": "PENDING",
                "tournamentId": "741"
              }
            ],
            "key": "Court 4",
            "name": "Court 4"
          }
        ],
        "key": "2026-06-23",
        "name": "今天"
      }
    ],
    "seed": [
      {
        "data": [
          {
            "country": {
              "code": "USA",
              "name": "美国"
            },
            "name": "Fritz",
            "playerId": "FB98",
            "seed": 1,
            "status": "ACTIVE",
            "tour": "ATP",
            "tournamentId": "741"
          }
        ],
        "type": "ATP"
      },
      {
        "data": [
          {
            "country": {
              "code": "ESP",
              "name": "西班牙"
            },
            "label": "32强",
            "name": "Munar",
            "playerId": "MU94",
            "seed": 7,
            "status": "ELIMINATED",
            "tour": "ATP",
            "tournamentId": "741"
          }
        ],
        "type": "OUT"
      }
    ]
  }
}
```

### `/finished` 响应示例

```json
{
  "code": 0,
  "data": {
    "match": [
      {
        "data": [
          {
            "id": "MS031",
            "player1": {
              "country": {
                "code": "BRA",
                "name": "巴西"
              },
              "id": "F0FV",
              "name": "Fonseca",
              "seed": 2
            },
            "player2": {
              "country": {
                "code": "BRA",
                "name": "巴西"
              },
              "id": "F0FV",
              "name": "Fonseca",
              "seed": 2
            },
            "round": "R32",
            "roundShow": "32强",
            "sets": [],
            "status": "FINISHED",
            "tournamentId": "741",
            "winnerId": "F0FV"
          }
        ],
        "key": "R32",
        "name": "32强"
      }
    ],
    "seed": [
      {
        "data": [
          {
            "country": {
              "code": "USA",
              "name": "美国"
            },
            "name": "Fritz",
            "playerId": "FB98",
            "seed": 1,
            "status": "ACTIVE",
            "tour": "ATP",
            "tournamentId": "741"
          }
        ],
        "type": "ATP"
      },
      {
        "data": [
          {
            "country": {
              "code": "ESP",
              "name": "西班牙"
            },
            "label": "32强",
            "name": "Munar",
            "playerId": "MU94",
            "seed": 7,
            "status": "ELIMINATED",
            "tour": "ATP",
            "tournamentId": "741"
          }
        ],
        "type": "OUT"
      }
    ]
  }
}
```
