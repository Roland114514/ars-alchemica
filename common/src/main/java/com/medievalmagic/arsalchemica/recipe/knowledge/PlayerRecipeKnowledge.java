package com.medievalmagic.arsalchemica.recipe.knowledge;

import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.*;

/**
 * 玩家炼金配方知识图谱与古代秘方造诣状态机。
 * <p>
 * 记录玩家已发现/破译的配方索引、古代注脚熟练度，并动态解算古代传奇配方的实际成功率。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class PlayerRecipeKnowledge {

    public static final int MAX_MASTERY_LEVEL = 5;
    public static final double MASTERY_BONUS_PER_LEVEL = 0.15; // 每级注脚 +15% 成功率

    private final Set<ResourceLocation> discoveredRecipes = new HashSet<>();
    private final Map<ResourceLocation, Integer> recipeMastery = new HashMap<>();

    public PlayerRecipeKnowledge() {}

    /**
     * 判定指定配方是否已解锁收录于手记中。
     */
    public boolean isDiscovered(ResourceLocation recipeId) {
        if (recipeId == null) return false;
        return this.discoveredRecipes.contains(recipeId);
    }

    /**
     * 解锁新配方。
     *
     * @return true 若为首次发现收录；false 若早已掌握。
     */
    public boolean unlockRecipe(ResourceLocation recipeId) {
        if (recipeId == null) return false;
        return this.discoveredRecipes.add(recipeId);
    }

    /**
     * 获取指定配方的古代造诣熟练度 (0 ~ 5)。
     */
    public int getMastery(ResourceLocation recipeId) {
        if (recipeId == null) return 0;
        return this.recipeMastery.getOrDefault(recipeId, 0);
    }

    /**
     * 研读古代注脚，提升特定配方的熟练度。
     *
     * @return true 若成功提升；false 若已达满级或参数非法。
     */
    public boolean increaseMastery(ResourceLocation recipeId, int amount) {
        if (recipeId == null || amount <= 0) return false;
        int current = getMastery(recipeId);
        if (current >= MAX_MASTERY_LEVEL) return false;

        int updated = Math.min(MAX_MASTERY_LEVEL, current + amount);
        this.recipeMastery.put(recipeId, updated);
        // 自动视作已知该配方
        this.discoveredRecipes.add(recipeId);
        return true;
    }

    /**
     * 动态博弈算式：计算古代传奇配方的最终调制成功率。
     * <p>
     * 公式：
     * P = BaseRate + (Mastery * 0.15) + (PurityBonus * 0.10)
     * 最终严格钳位在 [0.10, 0.95]。即使是神话配方，也有至少 10% 的最低奇迹机会与 5% 的绝对失控风险。
     * </p>
     *
     * @param recipe             目标古代配方
     * @param reagentPurityBonus 原料净度加成 [0.0 ~ 1.0]
     * @return 最终成功概率 [0.10 ~ 0.95]
     */
    public double calculateAncientSuccessChance(AlchemicalRecipe recipe, double reagentPurityBonus) {
        if (recipe == null) return 0.0;
        if (!recipe.isAncient()) {
            return 1.0; // 常规配方只要工序达标即 100% 成功
        }

        double base = recipe.getBaseSuccessRate();
        int mastery = getMastery(recipe.getRecipeId());
        double masteryBonus = mastery * MASTERY_BONUS_PER_LEVEL;
        double purityBonus = Mth.clamp(reagentPurityBonus, 0.0, 1.0) * 0.10;

        return Mth.clamp(base + masteryBonus + purityBonus, 0.10, 0.95);
    }

    public Set<ResourceLocation> getDiscoveredRecipes() {
        return Collections.unmodifiableSet(this.discoveredRecipes);
    }

    public Map<ResourceLocation, Integer> getRecipeMastery() {
        return Collections.unmodifiableMap(this.recipeMastery);
    }

    public void clear() {
        this.discoveredRecipes.clear();
        this.recipeMastery.clear();
    }

    // --- NBT 持久化 ---

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        CompoundTag discoveredTag = new CompoundTag();
        for (ResourceLocation id : this.discoveredRecipes) {
            discoveredTag.putBoolean(id.toString(), true);
        }
        tag.put("Discovered", discoveredTag);

        CompoundTag masteryTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : this.recipeMastery.entrySet()) {
            masteryTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("Mastery", masteryTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.discoveredRecipes.clear();
        this.recipeMastery.clear();

        if (tag == null) return;

        if (tag.contains("Discovered")) {
            CompoundTag discoveredTag = tag.getCompound("Discovered");
            for (String key : discoveredTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    this.discoveredRecipes.add(id);
                }
            }
        }

        if (tag.contains("Mastery")) {
            CompoundTag masteryTag = tag.getCompound("Mastery");
            for (String key : masteryTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    this.recipeMastery.put(id, masteryTag.getInt(key));
                }
            }
        }
    }
}
