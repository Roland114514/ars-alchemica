package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.herb.AlchemicalHerbBlock;
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

    // 7 大奇特魔法草药方块
    private static Supplier<AlchemicalHerbBlock> CINDER_THISTLE = () -> null;
    private static Supplier<AlchemicalHerbBlock> FROSTVEIN_ORCHID = () -> null;
    private static Supplier<AlchemicalHerbBlock> STORM_MARIGOLD = () -> null;
    private static Supplier<AlchemicalHerbBlock> ABYSSAL_LUMINA_MINT = () -> null;
    private static Supplier<AlchemicalHerbBlock> SCREAMING_MANDRAGORA = () -> null;
    private static Supplier<AlchemicalHerbBlock> MOONVEIL_BELLADONNA = () -> null;
    private static Supplier<AlchemicalHerbBlock> CARRION_BOGMOSS = () -> null;

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

    public static void bindHerbs(
            Supplier<AlchemicalHerbBlock> cinderThistle,
            Supplier<AlchemicalHerbBlock> frostveinOrchid,
            Supplier<AlchemicalHerbBlock> stormMarigold,
            Supplier<AlchemicalHerbBlock> abyssalMint,
            Supplier<AlchemicalHerbBlock> mandragora,
            Supplier<AlchemicalHerbBlock> belladonna,
            Supplier<AlchemicalHerbBlock> bogmoss
    ) {
        CINDER_THISTLE = Objects.requireNonNull(cinderThistle, "Cinder thistle supplier cannot be null");
        FROSTVEIN_ORCHID = Objects.requireNonNull(frostveinOrchid, "Frostvein orchid supplier cannot be null");
        STORM_MARIGOLD = Objects.requireNonNull(stormMarigold, "Storm marigold supplier cannot be null");
        ABYSSAL_LUMINA_MINT = Objects.requireNonNull(abyssalMint, "Abyssal mint supplier cannot be null");
        SCREAMING_MANDRAGORA = Objects.requireNonNull(mandragora, "Mandragora supplier cannot be null");
        MOONVEIL_BELLADONNA = Objects.requireNonNull(belladonna, "Belladonna supplier cannot be null");
        CARRION_BOGMOSS = Objects.requireNonNull(bogmoss, "Bogmoss supplier cannot be null");
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

    public static AlchemicalHerbBlock getCinderThistle() { return CINDER_THISTLE.get(); }
    public static AlchemicalHerbBlock getFrostveinOrchid() { return FROSTVEIN_ORCHID.get(); }
    public static AlchemicalHerbBlock getStormMarigold() { return STORM_MARIGOLD.get(); }
    public static AlchemicalHerbBlock getAbyssalLuminaMint() { return ABYSSAL_LUMINA_MINT.get(); }
    public static AlchemicalHerbBlock getScreamingMandragora() { return SCREAMING_MANDRAGORA.get(); }
    public static AlchemicalHerbBlock getMoonveilBelladonna() { return MOONVEIL_BELLADONNA.get(); }
    public static AlchemicalHerbBlock getCarrionBogmoss() { return CARRION_BOGMOSS.get(); }
}
