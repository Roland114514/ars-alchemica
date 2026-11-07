package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.DryingRackBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.HerbPressBlock;
import com.medievalmagic.arsalchemica.herb.apparatus.MortarBlock;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 跨平台 Block 延迟绑定中心 (Common-First)。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ModBlocks {

    private ModBlocks() {}

    private static Supplier<AlchemicalCauldronBlock> CAULDRON = () -> null;
    private static Supplier<MortarBlock> MORTAR = () -> null;
    private static Supplier<HerbPressBlock> HERB_PRESS = () -> null;
    private static Supplier<DryingRackBlock> DRYING_RACK = () -> null;

    public static void bindCauldron(Supplier<AlchemicalCauldronBlock> supplier) {
        CAULDRON = Objects.requireNonNull(supplier, "Cauldron supplier cannot be null");
    }

    public static void bindApparatus(
            Supplier<MortarBlock> mortar,
            Supplier<HerbPressBlock> press,
            Supplier<DryingRackBlock> dryingRack
    ) {
        MORTAR = Objects.requireNonNull(mortar, "Mortar supplier cannot be null");
        HERB_PRESS = Objects.requireNonNull(press, "Herb press supplier cannot be null");
        DRYING_RACK = Objects.requireNonNull(dryingRack, "Drying rack supplier cannot be null");
    }

    public static AlchemicalCauldronBlock getCauldron() {
        return CAULDRON.get();
    }

    public static MortarBlock getMortar() {
        return MORTAR.get();
    }

    public static HerbPressBlock getHerbPress() {
        return HERB_PRESS.get();
    }

    public static DryingRackBlock getDryingRack() {
        return DRYING_RACK.get();
    }
}
