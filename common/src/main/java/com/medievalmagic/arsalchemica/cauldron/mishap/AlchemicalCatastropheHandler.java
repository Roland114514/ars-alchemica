package com.medievalmagic.arsalchemica.cauldron.mishap;

import com.medievalmagic.arsalchemica.registry.ModEffects;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 炼金灾变与失控事故执行器 (Common-First)。
 * <p>
 * 负责在物理大世界中调度以太爆炸、火海引燃、复合有害瘴气云生成、方块损毁与副产物掉落。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalCatastropheHandler {

    private AlchemicalCatastropheHandler() {}

    /**
     * 触发并执行具体的炼金灾变。
     */
    public static void executeMishap(Level level, BlockPos pos, AlchemicalMishapType mishap) {
        if (level == null || level.isClientSide() || mishap == null || mishap == AlchemicalMishapType.NONE) {
            return;
        }

        switch (mishap) {
            case TOXIC_MIASMA_OUTBREAK -> executeToxicMiasma(level, pos);
            case INFERNAL_CONFLAGRATION -> executeConflagration(level, pos);
            case ARCANE_DETONATION -> executeArcaneDetonation(level, pos);
            case DRY_BOIL_MELTDOWN -> executeDryBoilMeltdown(level, pos);
            case SLUDGE_COAGULATION -> {
                level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.7f);
            }
            case CRYSTALLIZE_SUCCESS -> {
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.5f);
            }
        }
    }

    /**
     * 以太剧毒瘴气爆发：生成大范围滞留药水云，附加多重毁灭性 Debuff。
     */
    private static void executeToxicMiasma(Level level, BlockPos pos) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
        cloud.setRadius(4.5f);
        cloud.setDuration(300); // 15 秒滞留
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-0.005f);

        // 复合毒素：以太毒蚀 II + 凋零 + 致盲 + 神经迟缓
        cloud.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 140, 1));
        cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));

        level.addFreshEntity(cloud);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
        level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 0.7f, 0.8f);
    }

    /**
     * 炼狱烈焰蔓延：喷射火柱并在周边 3x3 范围引燃地表火。
     */
    private static void executeConflagration(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.2f, 0.9f);
        level.playSound(null, pos, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 1.0f, 1.0f);

        // 周边 3x3 引燃
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos target = pos.offset(dx, 0, dz);
                if (level.getBlockState(target).isAir() && level.getBlockState(target.below()).isSolidRender(level, target.below())) {
                    level.setBlock(target, Blocks.FIRE.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * 以太裂解大殉爆：高额爆炸伤害，彻底炸毁坩埚方块并散落废渣铁片。
     */
    private static void executeArcaneDetonation(Level level, BlockPos pos) {
        // 1. 产生破坏性以太爆炸 (半径 3.5)
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.5f, Level.ExplosionInteraction.BLOCK);

        // 2. 摧毁坩埚方块
        level.destroyBlock(pos, false);

        // 3. 散落 2~4 个炼金铁渣
        int slagCount = 2 + level.random.nextInt(3);
        ItemStack slagStack = new ItemStack(ModItems.getSlag(), slagCount);
        ItemEntity slagEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slagStack);
        level.addFreshEntity(slagEntity);
    }

    /**
     * 干烧过热熔毁：高温空烧导致金属爆裂崩解。
     */
    private static void executeDryBoilMeltdown(Level level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.2f, Level.ExplosionInteraction.BLOCK);
        level.destroyBlock(pos, false);

        // 散落 1~2 个炼金铁渣并在原位引燃火焰
        int slagCount = 1 + level.random.nextInt(2);
        ItemStack slagStack = new ItemStack(ModItems.getSlag(), slagCount);
        ItemEntity slagEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slagStack);
        level.addFreshEntity(slagEntity);

        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
        }
    }
}
