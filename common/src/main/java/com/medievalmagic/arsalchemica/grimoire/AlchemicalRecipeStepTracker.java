package com.medievalmagic.arsalchemica.grimoire;

import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;

/**
 * 炼金工艺工序动态追踪器。
 * <p>
 * 根据当前坩埚内热力学与药理状态，推导学者手记中的下一步工艺指引。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalRecipeStepTracker {

    private AlchemicalRecipeStepTracker() {}

    public enum RecipePhase {
        FILL_WATER("step.ars_alchemica.fill_water"),
        PREHEAT("step.ars_alchemica.preheat"),
        ADD_INGREDIENTS("step.ars_alchemica.add_ingredients"),
        STIRRING("step.ars_alchemica.stirring"),
        STABILIZE("step.ars_alchemica.stabilize"),
        EXTRACT("step.ars_alchemica.extract");

        private final String translationKey;

        RecipePhase(String key) {
            this.translationKey = key;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    /**
     * 根据坩埚当前物理状态推断工序阶段。
     */
    public static RecipePhase derivePhase(int waterLevel, double temperature, double mass, double instability, int totalTurns) {
        if (waterLevel <= 0) {
            return RecipePhase.FILL_WATER;
        }
        if (temperature < 60.0) {
            return RecipePhase.PREHEAT;
        }
        if (mass < 1.0) {
            return RecipePhase.ADD_INGREDIENTS;
        }
        if (instability >= 0.50) {
            return RecipePhase.STABILIZE;
        }
        if (totalTurns < 2) {
            return RecipePhase.STIRRING;
        }
        return RecipePhase.EXTRACT;
    }
}
