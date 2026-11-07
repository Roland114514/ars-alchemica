package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import com.medievalmagic.arsalchemica.registry.ModEffects;
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
 * 负责驱动服务端 Tick 衰减、过载药效剥夺反转、以太毒蚀 Debuff 施加及跨平台网络增量派发。
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
        if (player == null || (player.level() != null && player.level().isClientSide())
                || player.isCreative() || player.isSpectator() || player.isDeadOrDying()) return;

        getData(player).ifPresent(data -> {
            float currentTox = data.getToxicity();
            if (currentTox <= 0.0f && !data.isDirty()) return;

            float prevTox = currentTox;
            ToxicityThreshold prevThreshold = data.getThreshold();

            // 1. 连续代谢衰减与增益代谢率平滑复原
            data.tickMetabolism();

            ToxicityThreshold currentThreshold = data.getThreshold();

            // 2. 每 20 Ticks (1秒) 执行一次生理阈值症状判定，施加专属以太毒蚀 Debuff
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
     * 玩家死亡或重生事件钩子：清除体内全部毒素积累，避免重生无限暴毙循环。
     */
    public static void onPlayerDeathOrRespawn(Player player) {
        if (player == null) return;
        getData(player).ifPresent(data -> {
            data.resetOnDeath();
            syncToClient(player, data);
            data.setDirty(false);
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
            // 清除可能存在的以太毒蚀 Debuff
            player.removeEffect(ModEffects.getCorrosion());
            syncToClient(player, data);
            data.setDirty(false);
        });
    }

    private static void applyPhysiologicalReactions(Player player, ToxicityThreshold threshold) {
        switch (threshold) {
            case SAFE, CAUTION -> {
                // 安全期或警觉期（轻度暗角微晕，不施加侵蚀 Debuff）
            }
            case DANGER -> {
                // 中毒期 (75% ~ 90%): 施加 0 级以太毒蚀 (模仿剧毒，生命值 > 1.0f 结算)，伴随眩晕与迟缓
                player.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 60, 0, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            }
            case OVERDOSE -> {
                // 休克崩解 (> 90%): 剥夺所有正面增益，施加致死性 1 级以太毒蚀，直击生命本源
                purgeBeneficialEffects(player);
                player.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 60, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 1, false, true, true));
                // 同时直接执行单次过载反噬爆发伤害
                player.hurt(AlchemicalDamageSources.corrosion(player.level()), 2.5f);
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
