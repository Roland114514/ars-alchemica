package com.medievalmagic.arsalchemica.grimoire;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;

/**
 * 学者直觉 (Scholar's Intuition) 数据快照载体。
 * <p>
 * 当玩家副手装备《炼金手记》并凝视炼金坩埚时，由此快照向 HUD 传递实时的微观热力学与药理数据。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public record ScholarIntuitionData(
        double temperature,
        CauldronReactionState.HeatLevel heatLevel,
        int waterLevel,
        double mass,
        double purity,
        double instability,
        double turbidity,
        int clockwiseTurns,
        int counterClockwiseTurns,
        int fluidColor,
        AlchemyAspect dominantAspect,
        String recipeStepGuidance,
        double mapX,
        double mapY,
        String nearbyRecipeKey
) {

    public ScholarIntuitionData(
            double temperature,
            CauldronReactionState.HeatLevel heatLevel,
            int waterLevel,
            double mass,
            double purity,
            double instability,
            double turbidity,
            int clockwiseTurns,
            int counterClockwiseTurns,
            int fluidColor,
            AlchemyAspect dominantAspect,
            String recipeStepGuidance
    ) {
        this(temperature, heatLevel, waterLevel, mass, purity, instability, turbidity,
                clockwiseTurns, counterClockwiseTurns, fluidColor, dominantAspect,
                recipeStepGuidance, 0.0, 0.0, null);
    }

    /**
     * 空溶液/未激活坩埚的默认回退快照。
     */
    public static final ScholarIntuitionData EMPTY = new ScholarIntuitionData(
            20.0,
            CauldronReactionState.HeatLevel.COLD,
            0,
            0.0,
            1.0,
            0.0,
            0.0,
            0,
            0,
            0x385DC6,
            null,
            "step.ars_alchemica.empty_cauldron",
            0.0,
            0.0,
            null
    );

    /**
     * 判定当前溶液是否处于失控临界态 (危险警告)。
     */
    public boolean isHazardous() {
        return instability >= 0.70 || turbidity >= 0.75;
    }

    /**
     * 判定是否达到沸腾反应温度。
     */
    public boolean isBoiling() {
        return temperature >= 100.0;
    }
}
