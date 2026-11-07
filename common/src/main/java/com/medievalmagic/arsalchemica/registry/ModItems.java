package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 跨平台 Item 延迟绑定中心 (Common-First)。
 * <p>
 * 提供多加载器解耦的物品引用与工厂支持。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ModItems {

    private ModItems() {}

    private static Supplier<AlchemicalPotionItem> ELIXIR = () -> null;
    private static Supplier<AlchemicalPotionItem> INCENSE = () -> null;
    private static Supplier<BladeOilItem> BLADE_OIL = () -> null;

    public static void bindPotions(
            Supplier<AlchemicalPotionItem> elixirSupplier,
            Supplier<AlchemicalPotionItem> incenseSupplier,
            Supplier<BladeOilItem> bladeOilSupplier
    ) {
        ELIXIR = Objects.requireNonNull(elixirSupplier, "Elixir supplier cannot be null");
        INCENSE = Objects.requireNonNull(incenseSupplier, "Incense supplier cannot be null");
        BLADE_OIL = Objects.requireNonNull(bladeOilSupplier, "Blade oil supplier cannot be null");
    }

    public static AlchemicalPotionItem getElixir() {
        return ELIXIR.get();
    }

    public static AlchemicalPotionItem getIncense() {
        return INCENSE.get();
    }

    public static BladeOilItem getBladeOil() {
        return BLADE_OIL.get();
    }
}
