# Agent Guidelines for Minecraft Mega-Mod Project

## 1. 双轨大模型协作规范 (Dual-Model Protocol)
- **默认模式（3.8 Flash）**：在用户没有指明“Gemini4”时，默认使用当前对话环境的 Gemini 3.8 Flash 完成日常任务，不消耗用户的 API Key 额度。
- **Gemini 4 模式（API 调用）**：当用户明确要求“使用Gemini4工作”或“用Gemini4”时，必须自动调用本地 `python3 run_gemini4.py -p "..."` 或通过 `gemini_api` 模块调度 `gemini-4-argon`，将高阶推理、宏观架构规划或复杂算法方案融入项目。

## 2. 工程化与模组开发铁律 (Modding Principles)
- **Common First 架构**：核心游戏逻辑、物品方块定义必须保持跨平台抽象（放在 `common` 模块），杜绝将平台特有包（`net.neoforged.*` 或 `net.fabricmc.*`）硬编码在通用逻辑中。
- **性能优先 (Zero-Lag)**：
  - 严禁空闲 Block Entity 执行无意义的高频 tick。
  - 复杂搜索/网络拓扑计算必须使用相位取模（Staggered Ticking，如 `(gameTime + hashCode) % 20 == 0`）。
  - 邻近 Capability 必须缓存，仅在 `neighborChanged` 时失效刷新。
  - 网络包同步采用轻量化 Delta 同步，杜绝直接全量序列化 NBT。
- **双语本地化**：所有注册项、GUI 标题、Tooltip 描述必须同时同步生成 `zh_cn.json` 与 `en_us.json`。

## 3. 资源完整性与内容可见性闭环 (Resource Completeness & In-Game Visibility)
- **必须注册创造模式物品栏标签页 (Creative Tabs)**：
  - 每次新增任何物品或方块，必须同时在所有平台（NeoForge/Forge 和 Fabric）注册对应的专属创造模式物品栏（`CreativeModeTab`），并将所有新增物品（`Item` / `BlockItem`）按逻辑顺序注入标签页！
  - 绝对杜绝“仅注册物品却未加入创造标签页”，导致玩家进入创造模式时找不到任何新物品。
- **必须提供战利品表 (Loot Tables)**：
  - 每一个注册的方块必须在 `data/<modid>/loot_tables/blocks/` 下提供标准掉落表（默认掉落自身，支持精准采集与爆炸衰减）。
  - 探索类稀有物品（如配方残卷、古代羊皮纸）必须提供地牢/遗迹宝箱注入或专属箱子战利品表（`loot_tables/chests/`）。
- **必须提供标准配方 (Recipes)**：
  - 所有器械、工具、消耗品必须在 `data/<modid>/recipes/` 下提供原版工作台合成（`crafting_shaped` / `crafting_shapeless`）、熔炼（`smelting` / `blasting`）等配方，形成可玩闭环。
- **必须配置标签 (Tags)**：
  - 方块必须配置原版挖掘工具标签（`minecraft:mineable/pickaxe`、`minecraft:mineable/axe`）及挖掘等级（`minecraft:needs_stone_tool`、`minecraft:needs_iron_tool` 等）。
  - 物品必须配置合理的分类标签（`potions`、`oils`、`apparatus` 等）。
- **材质与模型闭环 (Zero Missing Models/Textures)**：
  - 所有注册物品必须提供对应的 `assets/<modid>/models/item/*.json` 与像素贴图，杜绝紫黑缺失材质方块或隐形物品。

