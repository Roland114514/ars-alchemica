package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.potion.AlchemicalCorrosionEffect;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 跨平台 MobEffect 延迟绑定中心 (Common-First)。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ModEffects {

    private ModEffects() {}

    private static final AlchemicalCorrosionEffect DEFAULT_CORROSION = new AlchemicalCorrosionEffect();
    private static Supplier<AlchemicalCorrosionEffect> CORROSION = () -> DEFAULT_CORROSION;

    public static void bindCorrosion(Supplier<AlchemicalCorrosionEffect> supplier) {
        CORROSION = Objects.requireNonNull(supplier, "Corrosion supplier cannot be null");
    }

    public static AlchemicalCorrosionEffect getCorrosion() {
        return CORROSION.get();
    }
}
