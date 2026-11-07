# 我的世界超大型模组工程 (Minecraft Mega-Mod Project)

本项目包含了超大型 Minecraft 模组开发脚手架，以及基于 Google Gemini API 的双轨大模型协作机制。

---

## 🤖 双轨大模型协作规范

在后续开发本模组的过程中，我们遵循以下协作机制：

1. **默认模式（额度模式 / 3.8 Flash）**
   - 默认由系统自带的 **Gemini 3.8 Flash** 提供服务。
   - 负责：日常代码编写、文件检索、常规问题解答、基础配置生成，**不消耗用户的 API Key 额度**。

2. **Gemini 4 模式（API 调用模式 / Gemini 4 Argon）**
   - 当你在对话中明确要求 **“使用Gemini4工作”** 或 **“用Gemini4”** 时，AI 将自动切换为**调用 API 模式**。
   - 系统将通过本地的 `run_gemini4.py` / `gemini_api` 模块直接调用当前最强推理模型 **`gemini-4-argon`**，进行：
     - 超大型模组的宏观架构设计
     - 复杂算法（如高维世界生成算法、复杂机械网络拓扑、自研多线程优化）
     - 关键重构与高难度 Debug
     - 系统级游戏策划与数学数值平衡设计

---

## 🛠️ Gemini API 本地程序说明

### 目录结构
- `.env`：存储用户的 Gemini API Key（已自动配置，受 `.gitignore` 保护）
- `gemini_api/client.py`：原生 REST API 客户端，无需安装额外 pip 依赖，内置 macOS SSL 修复
- `run_gemini4.py`：CLI 命令行执行器与交互式终端

### 使用示例
```bash
# 1. 命令行直接提问
python3 run_gemini4.py -p "你的提示词"

# 2. 从文件读取提示词并保存输出
python3 run_gemini4.py -f docs/architecture_prompt.txt -o docs/architecture_plan.md

# 3. 指定系统角色
python3 run_gemini4.py -s "你是一名资深Minecraft模组技术总监" -p "设计一个多方块机械结构网络协议"

# 4. 进入交互式对话终端
python3 run_gemini4.py --interactive
```
