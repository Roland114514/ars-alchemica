package net.minecraft.world.item.trading;

import net.minecraft.world.item.ItemStack;

public class MerchantOffer {
    private final ItemStack baseCostA;
    private final ItemStack costB;
    private final ItemStack result;
    private int uses;
    private final int maxUses;
    private final int xp;
    private final float priceMultiplier;

    public MerchantOffer(ItemStack baseCostA, ItemStack costB, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        this.baseCostA = baseCostA.copy();
        this.costB = costB.copy();
        this.result = result.copy();
        this.maxUses = maxUses;
        this.xp = xp;
        this.priceMultiplier = priceMultiplier;
        this.uses = 0;
    }

    public MerchantOffer(ItemStack baseCostA, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        this(baseCostA, ItemStack.EMPTY, result, maxUses, xp, priceMultiplier);
    }

    public ItemStack getBaseCostA() { return baseCostA; }
    public ItemStack getCostA() { return baseCostA; }
    public ItemStack getCostB() { return costB; }
    public ItemStack getResult() { return result; }
    public int getUses() { return uses; }
    public int getMaxUses() { return maxUses; }
    public int getXp() { return xp; }
    public float getPriceMultiplier() { return priceMultiplier; }
    public boolean isOutOfStock() { return uses >= maxUses; }
    public void increaseUses() { this.uses++; }
}
