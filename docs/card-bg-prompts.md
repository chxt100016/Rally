# 约球卡片底图 · 生成提示词清单

## 出图规格（关键）
- 风格：**精致的 3D 卡通渲染**，stylized 但可信（像皮克斯 / 高质量建筑可视化 / 精品手游场景），**不是抽象、不是简陋低模网页图标**。明亮鲜艳但有真实的材质、光影、细节。
- **具体真实的场景**：画完整可信的网球场（围网、球场线、网柱、灯杆、周边看台/绿植/长椅等环境），**不要抽象元素**（如"天空上飘一个网球"这类脱离场景的符号）。
- full-bleed 铺满，无渐变遮罩、**不压暗、不做四周暗角**。
- 视角：**3/4 斜俯视 / 等距（isometric / dynamic three-quarter）**，有纵深、有动感，禁止正视平视。动感来自场景本身（微微的运动模糊、地面反光、光线），而非贴一个抽象符号。
- 比例：**3.5:1**，推荐 **1400×400**。图用 aspectFill 铺满会裁上下 → 主体放**垂直居中带**，上下留安全边。
- 可读性靠**构图留白**：把天空/一侧地面留成大面积干净区域，文字压上去自然清楚（前端文字再叠很淡的白字阴影兜底）。

## 维度划分
球场拆成两个正交枚举：
- 表面材质 Surface：HARD / CLAY / GRASS
- 场地位置 Venue：OUTDOOR / INDOOR

底图逻辑：OUTDOOR 按时段+天气选图；INDOOR 忽略时段天气，直接 indoor-{surface}。

## 场景矩阵（共 12 张）
| 场地 | 白天·晴 | 白天·阴 | 白天·雨 | 黄昏 | 黄昏·雨 | 夜晚 | 夜晚·雨 |
|---|---|---|---|---|---|---|---|
| 室外硬地 outdoor hard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 室外红土 outdoor clay | ✓（仅这张兜底） | — | — | — | — | — | — |
| 室外草地 outdoor grass | ✓（仅这张兜底） | — | — | — | — | — | — |
| 室内硬地 indoor hard | 恒定光 1 张 | | | | | | |
| 室内红土 indoor clay | 恒定光 1 张 | | | | | | |
| 室内草地 indoor grass | 恒定光 1 张 | | | | | | |

> 只有室外硬地做全套时段天气（7 张）；红土/草地国内少，各只出 1 张兜底（白天晴）；室内无日夜/天气各 1 张。
> 合计：硬地 7 + 红土 1 + 草地 1 + 室内 3 = 12。缺档时代码兜底到同材质兜底图。

## 命名规则
`{surface}-{时段}[-天气].jpg`：hard-day / hard-day-cloudy / hard-day-rain / hard-dusk / hard-dusk-rain / hard-night / hard-night-rain / clay-day / grass-day / indoor-hard / indoor-clay / indoor-grass。
backgroundImage 字段由后端用枚举类算好返回，前端傻展示。

---

## 风格前缀（所有室外图共用，直接拼在每段前面）
```
A high-quality, detailed 3D cartoon render of a realistic tennis court scene, for a full-bleed app card.
Stylized but believable, Pixar-quality / premium mobile-game environment art — NOT abstract, NOT a flat icon, NOT a cheap low-poly render.
Wide ultra-panoramic landscape, aspect ratio 3.5:1 (1400x400).
Dynamic three-quarter isometric bird's-eye view with real depth, perspective and energy — never a flat front view.
Render a complete, plausible court: fenced court, painted lines, net and net posts, and surrounding environment (bleachers, trees, benches, light poles as fitting).
Rich materials and textures, soft global illumination, gentle ambient occlusion, subtle reflections, clean polished finish, crisp edges, fine detail.
Bright, saturated, cheerful colors. Keep one large clean area (sky or a stretch of ground) for overlaid text.
No people, no text, no logos, no watermarks. Do NOT darken the image, no vignette. Avoid: abstract symbols, floating objects, flat vector, plastic toy look, blurry, ugly, simplistic, low detail.
```
中文含义：高质量、精细的 3D 卡通渲染，画一个真实可信的网球场场景，用于铺满整卡；stylized 但可信，皮克斯级 / 精品手游场景美术——**不抽象、不做扁平图标、不做廉价低模**；3.5:1 超宽；3/4 斜等距俯视、有真实纵深透视和活力，绝不平视；画完整可信的球场（围网、球场线、网和网柱、周边环境如看台/树/长椅/灯杆）；丰富材质纹理、柔和全局光、轻环境光遮蔽、细腻反光、干净抛光质感、清晰边缘、精细细节；明亮高饱和欢快配色；保留一大片干净区域（天空或一段地面）给叠字；无人物/文字/logo/水印；不压暗、不暗角；避免：抽象符号、漂浮物体、扁平矢量、塑料玩具感、模糊、丑、简陋、低细节。

---

## 室外 · 硬地（blue/green hard court）

> 每段主体统一为卡通化的蓝绿色硬地（glossy blue/green hard court, crisp white lines and a net），光线/天气部分保持明亮插画感，不写实、不压暗。

### 硬地 · 白天晴
```
[风格前缀] A detailed blue/green hard court with crisp white lines, net and net posts.
Bright sunny midday, vivid blue sky with a few soft volumetric clouds, warm cheerful sunlight, soft realistic shadows.
```
中文：精细的蓝绿硬地，清晰白线、球网和网柱；明媚正午，鲜亮蓝天几朵柔和体积云，温暖欢快阳光，柔和真实阴影。

### 硬地 · 白天阴
```
[风格前缀] A glossy blue/green hard court with white lines and a net.
Soft overcast day, light pastel-grey sky, gentle even light, still bright and cheerful (not gloomy), cool pastel tones.
```
中文：光泽感蓝绿硬地，白线球网；柔和阴天，浅灰粉彩天空，均匀柔光，依旧明亮欢快（不阴郁），冷粉彩调。

### 硬地 · 白天雨
```
[风格前缀] A glossy blue/green hard court with a shiny wet surface and cute round raindrops.
Playful rainy day, fresh blue-grey sky, a few stylized clouds and light rain streaks, bright and lively, colorful puddles reflecting the sky.
```
中文：光泽感蓝绿硬地，亮亮的湿地面和圆润可爱雨滴；活泼雨天，清新蓝灰天空，几朵风格化云和细雨线，明亮有生气，水洼映着天空色彩。

### 硬地 · 黄昏
```
[风格前缀] A glossy blue/green hard court with white lines and a net.
Warm sunset, vibrant orange-pink-purple gradient sky, glowing golden light, cheerful and dreamy, still bright and colorful.
```
中文：光泽感蓝绿硬地，白线球网；温暖日落，鲜艳橙粉紫渐变天空，金色暖光，欢快梦幻，依旧明亮多彩。

### 硬地 · 黄昏雨
```
[风格前缀] A glossy blue/green hard court with a shiny wet surface and cute raindrops.
Warm rainy sunset, orange-and-violet sky with playful clouds and light rain, glowing golden highlights, puddles reflecting sunset colors, bright and lively.
```
中文：光泽感蓝绿硬地，亮湿地面和可爱雨滴；温暖雨中日落，橙紫天空配俏皮云和细雨，金色高光，水洼映日落色，明亮有生气。

### 硬地 · 夜晚
```
[风格前缀] A glossy blue/green hard court under glowing stadium floodlights at night.
Deep blue starry sky, bright warm floodlight pools, cheerful glowing lamps, vivid and colorful night scene (not dark or gloomy).
```
中文：光泽感蓝绿硬地，夜间明亮泛光灯下；深蓝星空，明亮暖色灯光斑，欢快发光的灯，鲜艳多彩的夜景（不暗不阴郁）。

### 硬地 · 夜晚雨
```
[风格前缀] A glossy blue/green hard court under glowing floodlights at night, shiny wet surface with colorful reflections.
Deep blue rainy night, bright warm lamp glow, playful light rain streaks, puddles reflecting the colorful lights, vivid and lively (not dark).
```
中文：光泽感蓝绿硬地，夜间明亮泛光灯下，亮湿地面映彩色光；深蓝雨夜，明亮暖灯光，俏皮细雨线，水洼映彩灯，鲜艳有生气（不暗）。

---

## 室外 · 红土
> 把上面硬地 7 段里的主体 `A glossy blue/green hard court...` 全部替换为下句，其余（光线/天气/天空）不变：
```
a glossy terracotta clay court (bright reddish-orange surface) with crisp white lines and a net
```
中文：明亮赭红色红土场（鲜红橙色表面）+ 白线和球网，保持卡通光泽感。红土同样出 7 张：晴/阴/雨/黄昏/黄昏雨/夜晚/夜晚雨。
> 注意：红土雨天地面偏深红湿润而非镜面反光，可把 `shiny wet surface` 换成 `damp deep-red clay surface with colorful puddles`（潮湿变深的红土配彩色水洼）。

---

## 室外 · 草地（仅白天晴一张兜底）
```
[风格前缀] a lush manicured grass court, bright striped green lawn, crisp white lines and a net.
Bright sunny midday, vivid blue sky with fluffy cartoon clouds, cheerful sunlight, playful soft shadows.
```
中文：茂盛整齐的草地场，明亮条纹绿草坪，白线球网；明媚正午，鲜亮蓝天配蓬松卡通云，欢快阳光，俏皮柔影。

---

## 室内（恒定人工光，各 1 张，无日夜无天气）
> 室内用下面这段室内前缀（同样明亮卡通、3/4 斜俯视、不压暗）。

室内前缀：
```
A high-quality, detailed 3D cartoon render of a realistic indoor tennis hall, for a full-bleed app card.
Stylized but believable, Pixar-quality / premium mobile-game environment art — NOT abstract, NOT a flat icon, NOT a cheap low-poly render.
Wide ultra-panoramic landscape, aspect ratio 3.5:1 (1400x400).
Dynamic three-quarter isometric bird's-eye view with real depth, perspective and energy — never a flat front view.
Render a complete, plausible indoor hall: full court with painted lines, net and posts, glowing ceiling lights, roof trusses and back wall, no windows.
Rich materials and textures, soft global illumination, gentle ambient occlusion, subtle reflections on the floor, clean polished finish, fine detail.
Bright, cheerful indoor lighting and saturated colors. Keep one large clean area (ceiling or floor) for overlaid text.
No people, no text, no logos, no watermarks. Do NOT darken the image, no vignette. Avoid: abstract symbols, floating objects, flat vector, plastic toy look, blurry, ugly, simplistic, low detail.
```
中文：高质量、精细的 3D 卡通渲染，画真实可信的室内网球馆，铺满整卡；stylized 但可信，皮克斯级 / 精品手游场景美术——不抽象、不做扁平图标、不做廉价低模；3.5:1 超宽；3/4 斜等距俯视、有真实纵深透视和活力，绝不平视；画完整可信的室内馆（完整球场带线、网和网柱、发光顶灯、屋顶桁架和后墙、无窗）；丰富材质纹理、柔和全局光、轻环境光遮蔽、地面细腻反光、干净抛光质感、精细细节；明亮欢快室内光和高饱和配色；保留一大片干净区域（顶棚或地面）给叠字；无人物/文字/logo/水印；不压暗、不暗角；避免：抽象符号、漂浮物体、扁平矢量、塑料玩具感、模糊、丑、简陋、低细节。

### 室内硬地
在室内前缀基础上，主体：`a glossy blue/green hard court with crisp white lines and a net`（光泽感蓝绿硬地）。

### 室内红土
主体替换为：`a glossy terracotta clay court (bright reddish-orange) with crisp white lines and a net`（明亮赭红红土）。

### 室内草地
主体替换为：`a lush manicured grass court with bright striped green lawn and a net`（明亮条纹草地）。

