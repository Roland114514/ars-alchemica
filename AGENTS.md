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
