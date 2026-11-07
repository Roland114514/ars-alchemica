你现在是《Ars Alchemica》（针对 Minecraft 1.20.1，Mojang Official Mappings）的核心首席架构师。
请按照《Ars Alchemica》的世界观与工程规范，为 **Milestone 3：动态药水系统与毒性槽引擎 (Dynamic Potions & Toxicity Engine)** 提供完整的架构设计与生产级代码实现。

【设计原则与铁律】：
1. **Common-First 跨平台原则**：
   - 所有核心业务逻辑、毒性代谢计算、动态药水与剑油效果运算全部放在 `common` 模块；
   - 玩家毒性数据必须提供平台中立的抽象契约接口（如 `IToxicityHandler` 或 `PlayerToxicityData`），方便未来在 Forge（Capability）与 Fabric/NeoForge（Data Attachment）中单点适配。
2. **猎魔人风格毒性机制 (Witcher-style Toxicity Engine)**：
   - 毒性槽区间 `[0.0, 100.0]`，支持最大上限动态扩展；
   - 毒性代谢方程：每个游戏刻（Tick）平滑代谢衰减：`dTox/dt = -metabolismRate`，支持解毒剂（如腐水食腐苔提取物）瞬间或加速净化；
   - 四大毒性阈值状态（ToxicityThreshold）：
     * `SAFE` (< 50%)：安全期，药效正常发挥；
     * `CAUTION` (50% ~ 75%)：警觉期，轻微毒副反应，客户端屏幕泛出微暗毒晕；
     * `DANGER` (75% ~ 90%)：中毒期，视野晃动（Nausea）、反胃、移动减速（Slowness）；
     * `OVERDOSE` (> 90%)：休克崩解，药水增益全部失效并反转为凋零/剧毒伤害，每秒造成无视护甲的炼金真实毒性反噬伤害！
3. **动态复合药水系统 (Continuous Aspect Potion Engine)**：
   - **彻底告别原版静态 Potion 枚举绑定**！药水必须支持基于连续以太向量（`AspectVector`）的动态效果解算：
     * 主导元素决定核心药效（如 Ignis -> 抗火/炽烈打击，Aqua -> 水下呼吸/疾速恢复，Vita -> 生命再生/额外生命，Mortis -> 伤害/剧毒，Aether -> 悬浮/奥术抵抗等）；
     * 纯度（Purity）决定药效等级（Amplifier: I, II, III...）；
     * 溶液质量（Mass）决定持续时间（Duration in ticks）；
     * 挥发性（Volatility）决定即时生效还是缓慢释放；
     * 颜色由 `AspectVector.getBlendedColor()` 动态光子混合产生。
   - **支持多种交付形态（Delivery Type）**：
     * 饮用药剂 (Elixir/Potion)
     * 涂抹剑油 (Blade Oil)：涂抹在主手武器上，赋予一定次数（如 30 次击中）的种族特攻或附魔毒素穿透（银刃死灵特攻、昆虫破壳特攻、类人生物流血等）；
     * 炼金熏香 (Incense)：范围释放场。
4. **轻量化二进制网络同步 (Zero-Lag)**：
   - 毒性网络包采用紧凑二进制序列化（Float tox + Byte stateMask），杜绝 NBT 字符串冗余。
5. **严苛边界防护**：
   - 防死锁、防除零、防数值溢出（Clamp）、线程安全。

请按模块输出完整的、生产级的 Java 实现代码与对应架构解析，涵盖：
1. `IToxicityData.java` 与 `PlayerToxicityManager.java` (毒性核心与代谢方程)
2. `AlchemicalPotionItem.java` (动态复合药剂载体，动态 Tooltip 与颜色提供器)
3. `BladeOilItem.java` (涂抹剑油机制与武器攻击次数扣减逻辑)
4. `ToxicitySyncPacket.java` (轻量化网络同步载荷)
5. 对应双语本地化键名约定。
