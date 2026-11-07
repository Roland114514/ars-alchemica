package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlockEntity;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 跨平台 BlockEntityType 延迟绑定中心 (Common-First)。
 * <p>
 * 允许 NeoForge / Fabric / Architectury 加载器在模组初始化阶段注入平台注册项，
 * 同时在无加载器单元测试环境下提供安全回退，彻底杜绝 {@code super(null, ...)} 引发的 NPE 崩溃。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    private static Supplier<BlockEntityType<MortarBlockEntity>> MORTAR = () -> null;
    private static Supplier<BlockEntityType<HerbPressBlockEntity>> HERB_PRESS = () -> null;
    private static Supplier<BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = () -> null;
    private static Supplier<BlockEntityType<AlchemicalCauldronBlockEntity>> CAULDRON = () -> null;

    /**
     * 各平台加载器在实体注册阶段调用此方法完成绑定。
     */
    public static void bind(
            Supplier<BlockEntityType<MortarBlockEntity>> mortarSupplier,
            Supplier<BlockEntityType<HerbPressBlockEntity>> pressSupplier,
            Supplier<BlockEntityType<DryingRackBlockEntity>> dryingRackSupplier
    ) {
        MORTAR = Objects.requireNonNull(mortarSupplier, "Mortar supplier cannot be null");
        HERB_PRESS = Objects.requireNonNull(pressSupplier, "Herb press supplier cannot be null");
        DRYING_RACK = Objects.requireNonNull(dryingRackSupplier, "Drying rack supplier cannot be null");
    }

    public static void bindCauldron(Supplier<BlockEntityType<AlchemicalCauldronBlockEntity>> cauldronSupplier) {
        CAULDRON = Objects.requireNonNull(cauldronSupplier, "Cauldron supplier cannot be null");
    }

    public static BlockEntityType<MortarBlockEntity> getMortarType() {
        return MORTAR.get();
    }

    public static BlockEntityType<HerbPressBlockEntity> getHerbPressType() {
        return HERB_PRESS.get();
    }

    public static BlockEntityType<DryingRackBlockEntity> getDryingRackType() {
        return DRYING_RACK.get();
    }

    public static BlockEntityType<AlchemicalCauldronBlockEntity> getCauldronType() {
        return CAULDRON.get();
    }
}
