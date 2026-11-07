package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 玩家毒性核心管理器与生理代谢调度引擎。
 * <p>
 * 负责驱动服务端 Tick 衰减、过载药效剥夺反转及跨平台网络增量派发。
 * 遵循严格的 Common-First 规范，解耦平台特异性 Capability / Data Attachment 实现。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class PlayerToxicityManager {

    private static Function<Player, Optional<IToxicityData>> provider = p -> Optional.empty();
    private static BiConsumer<Player, ToxicitySyncPacket> packetSender = (p, pkt) -> {};

    private PlayerToxicityManager() {}

    /**
     * 由各平台加载器在初始化时注入具体的 Capability 或 Data Attachment 获取器与发包器。
     */
    public static void initPlatformBridge(
            Function<Player, Optional<IToxicityData>> dataProvider,
            BiConsumer<Player, ToxicitySyncPacket> networkSender) {
        if (dataProvider != null) provider = dataProvider;
        if (networkSender != null) packetSender = networkSender;
    }

    public static Optional<IToxicityData> getData(Player player) {
        if (player == null) return Optional.empty();
        return provider.apply(player);
    }

    /**
     * 服务端玩家周期 Tick（在玩家每 tick 结束前调用）。
     * 执行连续时间代谢衰减及生理阈值症状结算。
     */
    public static void tickPlayer(Player player) {
        if (player == null || (player.level() != null && player.level().isClientSide()) || player.isCreative()) return;

        getData(player).ifPresent(data -> {
            float currentTox = data.getToxicity();
            if (currentTox <= 0.0f && !data.isDirty()) return;

            float prevTox = currentTox;
            ToxicityThreshold prevThreshold = data.getThreshold();

            // 1. 连续代谢衰减: dTox/dt = -metabolismRate
            if (currentTox > 0.0f) {
                data.addToxicity(-data.getMetabolismRate());
            }

            ToxicityThreshold currentThreshold = data.getThreshold();

            // 2. 每 20 Ticks (1秒) 执行一次生理阈值症状判定与伤害结算，杜绝高频卡顿与秒杀
            if (player.tickCount % 20 == 0 && data.getToxicity() > 0.0f) {
                applyPhysiologicalReactions(player, currentThreshold);
            }

            // 3. 轻量化网络增量同步（阈值跨跃、数值归零或突变 >= 0.5f，或者定时40tick保底）
            boolean thresholdChanged = (prevThreshold != currentThreshold);
            boolean significantDelta = Math.abs(prevTox - data.getToxicity()) >= 0.5f;
            boolean zeroedOut = (prevTox > 0.0f && data.getToxicity() == 0.0f);

            if (data.isDirty() || thresholdChanged || significantDelta || zeroedOut || player.tickCount % 40 == 0) {
                syncToClient(player, data);
                data.setDirty(false);
            }
        });
    }

    /**
     * 解毒剂净化接口（如腐水食腐苔浸出液）。
     *
     * @param instantPurge 瞬间消除的毒性量
     * @param boostedMetabolism 加速后的代谢速率
     */
    public static void applyAntidote(Player player, float instantPurge, float boostedMetabolism) {
        if (player == null) return;
        getData(player).ifPresent(data -> {
            if (instantPurge > 0.0f) {
                data.addToxicity(-instantPurge);
            }
            if (boostedMetabolism > data.getMetabolismRate()) {
                data.setMetabolismRate(boostedMetabolism);
            }
            syncToClient(player, data);
            data.setDirty(false);
        });
    }

    private static void applyPhysiologicalReactions(Player player, ToxicityThreshold threshold) {
        switch (threshold) {
            case SAFE, CAUTION -> {
                // 安全期或警觉期（轻度暗角警觉，不附加强制负面效果）
            }
            case DANGER -> {
                // 中毒期 (75% ~ 90%): 视野晃动与神经迟缓
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            }
            case OVERDOSE -> {
                // 休克崩解 (> 90%): 剥夺所有有益药水增益，反转为凋零与剧毒，并造成真实炼金反噬伤害
                purgeBeneficialEffects(player);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1, false, true, true));
                player.hurt(player.damageSources().magic(), 2.5f);
            }
        }
    }

    /**
     * 剥夺所有有益增益（快照拷贝防止并发修改异常）。
     */
    private static void purgeBeneficialEffects(Player player) {
        List<MobEffectInstance> toRemove = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect() != null && inst.getEffect().isBeneficial()) {
                toRemove.add(inst);
            }
        }
        for (MobEffectInstance inst : toRemove) {
            player.removeEffect(inst.getEffect());
        }
    }

    public static void syncToClient(Player player, IToxicityData data) {
        if (player == null || data == null) return;
        byte stateMask = (byte) (data.getThreshold().ordinal() & 0x03);
        packetSender.accept(player, new ToxicitySyncPacket(data.getToxicity(), data.getMaxToxicity(), stateMask));
    }
}
