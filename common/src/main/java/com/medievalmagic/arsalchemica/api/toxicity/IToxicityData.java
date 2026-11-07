package com.medievalmagic.arsalchemica.api.toxicity;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家炼金毒性数据契约接口（Common-First 跨平台抽象）。
 * <p>
 * 提供解耦的数据操作标准，使 Common 模块无需关心 Forge Capability 或 Fabric Attachment 实现细节。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public interface IToxicityData {
    float DEFAULT_MAX_TOXICITY = 100.0f;
    /** 默认自然代谢速率：每秒衰减 0.25 点毒性 (即每 Tick 0.0125f) */
    float DEFAULT_METABOLISM_RATE = 0.0125f;

    float getToxicity();
    void setToxicity(float value);
    void addToxicity(float delta);

    float getMaxToxicity();
    void setMaxToxicity(float max);

    float getMetabolismRate();
    void setMetabolismRate(float rate);

    /** 获取归一化毒性比例 [0.0, 1.0] */
    default float getToxicityRatio() {
        float max = getMaxToxicity();
        return max <= 1e-5f ? 0.0f : Math.min(1.0f, Math.max(0.0f, getToxicity() / max));
    }

    /** 获取当前生理阈值阶段 */
    default ToxicityThreshold getThreshold() {
        return ToxicityThreshold.fromRatio(getToxicityRatio());
    }

    /** 服务端自然代谢 Tick（包含毒性递减与增益代谢速率平滑回归） */
    void tickMetabolism();

    /** 玩家死亡重生重置逻辑（杜绝死后无限暴毙死循环） */
    void resetOnDeath();

    /** 标记是否需要向客户端发送二进制同步包 */
    boolean isDirty();
    void setDirty(boolean dirty);

    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
}
