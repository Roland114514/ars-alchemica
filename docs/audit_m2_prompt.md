你现在是《Ars Alchemica》（针对 Minecraft 1.20.1，Mojang Official Mappings）的核心首席架构师与资深 Mod 审查官。
请以极其严苛、挑剔、生产级工程标准的眼光，对 Milestone 2（草药生态底层与前置物理加工器械）的所有代码进行全面白盒深度自我审查（Self-Audit & Hardening）。

【审查目标代码清单】：
1. `common/src/main/java/com/medievalmagic/arsalchemica/herb/IAlchemicalHerb.java`
2. `common/src/main/java/com/medievalmagic/arsalchemica/herb/HerbRegistry.java`
3. `common/src/main/java/com/medievalmagic/arsalchemica/herb/AlchemicalHerbBlock.java` (含 7 大特种草药方块子类)
4. `common/src/main/java/com/medievalmagic/arsalchemica/herb/apparatus/ProcessedHerbItem.java`
5. `common/src/main/java/com/medievalmagic/arsalchemica/herb/apparatus/MortarBlock.java` & `MortarBlockEntity.java`
6. `common/src/main/java/com/medievalmagic/arsalchemica/herb/apparatus/HerbPressBlock.java` & `HerbPressBlockEntity.java`
7. `common/src/main/java/com/medievalmagic/arsalchemica/herb/apparatus/DryingRackBlock.java` & `DryingRackBlockEntity.java`
8. 双语语言文件 `zh_cn.json` 与 `en_us.json`。

【审查重点维度】：
1. **Minecraft 1.20.1 Mojang 官方映射准确性**：
   - 检查方法名、字段名是否在 1.20.1 中完全正确（例如 `MobEffects.CONFUSION` vs `MobEffects.NAUSEA`，`MobEffects.MOVEMENT_SLOWDOWN` vs `MobEffects.SLOWNESS`，`Style.withColor(TextColor.fromRgb(...))`，`DamageSources` 调用方式，`Level.getMoonPhase()`，`Biome.getBaseTemperature()` 等）。
2. **空指针与边界隐患 (NPE & Edge Cases)**：
   - BlockEntity 的构造器 `super(null, pos, state)` 在真实运行时或存档加载时的 NPE 风险，如何优雅解耦与注册绑定；
   - NBT 读写（`saveAdditional`/`load`）是否防御了空 tag、损坏 tag，物品反序列化 `ItemStack.of(...)` 的安全性；
   - 破坏方块掉落物（`onRemove`）是否可能发生物品重复掉落（Dupe bug）或吞物品；
   - 研磨石臼 `grindCount` 溢出处理与冷却节流是否可被高频宏（Auto-clicker）击穿；
   - 压榨台与风干架在多玩家同时右键/空手/切换物品时的并发状态机竞争。
3. **Common-First 跨平台与注册架构**：
   - 当前是否缺少统一的注册表管理器（如 `ModBlocks`, `ModItems`, `ModBlockEntities`）？如何以 Common-First 架构提供优雅的注册定义，使得 NeoForge / Fabric / Architectury 都能平滑接入？
4. **性能与 Zero-Lag 规范**：
   - 方块实体 tick、粒子发射频率、无意义对象创建、GC 开销审查。
5. **炼金数值与向量交互一致性**：
   - 7 种草药的 5 种形态在 `AspectVector` 变换、纯度 `calculatePurity()`、挥发性、毒性解算上是否与 Milestone 1 规则完全闭环，是否存在数值异常或逻辑矛盾。

请列出所有发现的隐患、缺陷和需要重构的点，并给出具体的生产级修复与加固代码。
