package com.medievalmagic.arsalchemica.cauldron;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.MishapResult;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalRecipeStepTracker;
import com.medievalmagic.arsalchemica.grimoire.ScholarIntuitionData;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb;
import com.medievalmagic.arsalchemica.herb.apparatus.ProcessedHerbItem;
import com.medievalmagic.arsalchemica.potion.AlchemicalDamageSources;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipe;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipeRegistry;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerKnowledgeManager;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerRecipeKnowledge;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModEffects;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 浸入式多态温控大坩埚方块实体 (Alchemical Cauldron BlockEntity)。
 * <p>
 * 驱动微观流体热力学演进、物理投料吸收入水、双向水动力学搅拌、事故反噬触发及灵药分装萃取。
 * 遵循严格的 Common-First 规范与零卡顿 (Zero-Lag) 相位取模设计。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalCauldronBlockEntity extends BlockEntity {

    public static final int MAX_WATER_LEVEL = 3;
    public static final int DEFAULT_FLUID_COLOR = 0x385DC6;

    private final CauldronReactionState reactionState = new CauldronReactionState();
    private int waterLevel = 0;
    private int fluidColor = DEFAULT_FLUID_COLOR;

    public AlchemicalCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.getCauldronType(), pos, state);
    }

    /**
     * 服务端主 Tick 调度：包含热力学演进、热源探测、跌落物品吸收与事故判定。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalCauldronBlockEntity be) {
        if (level == null || level.isClientSide()) return;

        // 1. 探测底部热源环境
        HeatLevel heatLevel = detectHeatSource(level, pos.below());
        boolean hasHeat = (heatLevel != HeatLevel.COLD);

        // 2. 演进热力学与微观以太状态机
        be.reactionState.tick(hasHeat, heatLevel);

        // 3. 更新溶液流体混色
        AspectVector currentVec = be.reactionState.getCurrentVector();
        if (!currentVec.isEmpty()) {
            be.fluidColor = currentVec.calculateBlendedColor();
        } else {
            be.fluidColor = DEFAULT_FLUID_COLOR;
        }

        // 4. 相位取模：每 4 Ticks 扫描一次坩埚内部沉浸的掉落物实体
        if ((level.getGameTime() + pos.hashCode()) % 4 == 0) {
            be.scanAndAbsorbDroppedItems(level, pos);
        }

        // 5. 炼金事故与结晶检查
        MishapResult mishap = be.reactionState.checkMishap();
        if (mishap != MishapResult.NONE) {
            be.handleMishap(level, pos, mishap);
        }

        // 6. 状态更新与轻量同步
        int currentHeatTier = heatLevel.ordinal();
        if (state.hasProperty(AlchemicalCauldronBlock.HEAT_TIER) && state.getValue(AlchemicalCauldronBlock.HEAT_TIER) != currentHeatTier) {
            level.setBlock(pos, state.setValue(AlchemicalCauldronBlock.HEAT_TIER, currentHeatTier), 3);
        }
    }

    /**
     * 探测下方热源方块。
     */
    public static HeatLevel detectHeatSource(Level level, BlockPos heatPos) {
        if (level == null) return HeatLevel.COLD;
        BlockState belowState = level.getBlockState(heatPos);

        // 星界极炎：灵魂营火或以太火
        if (belowState.is(Blocks.SOUL_FIRE) || (belowState.is(Blocks.SOUL_CAMPFIRE) && belowState.getValue(CampfireBlock.LIT))) {
            return HeatLevel.ASTRAL_INFERNO;
        }
        // 沸腾热源：岩浆或岩浆块
        if (belowState.is(Blocks.LAVA) || belowState.is(Blocks.MAGMA_BLOCK)) {
            return HeatLevel.ROILING_BOIL;
        }
        // 文火慢炖：普通火焰或普通营火
        if (belowState.is(Blocks.FIRE) || (belowState.is(Blocks.CAMPFIRE) && belowState.getValue(CampfireBlock.LIT))) {
            return HeatLevel.WARM_SIMMER;
        }

        return HeatLevel.COLD;
    }

    /**
     * 玩家右键交互逻辑。
     */
    public InteractionResult onPlayerInteract(Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.level == null) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // 1. 水桶注水
        if (held.is(Items.WATER_BUCKET)) {
            if (this.waterLevel < MAX_WATER_LEVEL) {
                if (!player.isCreative()) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
                setWaterLevel(MAX_WATER_LEVEL);
                this.fluidColor = DEFAULT_FLUID_COLOR;
                this.level.playSound(player, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.sidedSuccess(this.level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        // 2. 搅拌棒物理搅拌 (顺时针平息失稳 / 逆时针剪切排浊)
        if (held.getItem() instanceof AlchemicalStirringRodItem) {
            if (this.waterLevel > 0) {
                boolean clockwise = !player.isShiftKeyDown();
                this.reactionState.stir(clockwise, 0.28);
                this.level.playSound(player, this.worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, clockwise ? 1.2f : 0.8f);
                return InteractionResult.sidedSuccess(this.level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        // 3. 空玻璃瓶取样 / 灵药萃取
        if (held.is(Items.GLASS_BOTTLE)) {
            if (this.waterLevel > 0) {
                ItemStack potionResult;
                AspectVector currentVec = this.reactionState.getCurrentVector();
                double mass = currentVec.getTotalAspectMass();

                if (mass > 0.2) {
                    double purity = currentVec.calculatePurity() * (1.0 - this.reactionState.getTurbidity());
                    double x = currentVec.getX();
                    double y = currentVec.getY();
                    HeatLevel heat = this.reactionState.getActiveHeatLevel();
                    int cw = this.reactionState.getClockwiseTurns();
                    int ccw = this.reactionState.getCounterClockwiseTurns();

                    java.util.Optional<AlchemicalRecipe> matchOpt = AlchemicalRecipeRegistry.getInstance()
                            .findMatchingRecipe(x, y, heat, cw, ccw, purity);

                    if (matchOpt.isPresent()) {
                        AlchemicalRecipe recipe = matchOpt.get();
                        PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.getKnowledge(player);

                        if (recipe.isAncient()) {
                            double chance = knowledge.calculateAncientSuccessChance(recipe, purity);
                            double roll = this.level.random.nextDouble();
                            if (roll > chance) {
                                // 古代炼制反噬爆炸
                                handleMishap(this.level, this.worldPosition, MishapResult.ARCANE_EXPLOSION);
                                player.displayClientMessage(
                                        Component.translatable("message.ars_alchemica.ancient_craft_failed",
                                                Component.translatable(recipe.getTranslationKey()))
                                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                        false
                                );
                                return InteractionResult.sidedSuccess(this.level.isClientSide());
                            } else {
                                potionResult = recipe.createResult(mass, purity);
                                this.level.playSound(null, this.worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                player.displayClientMessage(
                                        Component.translatable("message.ars_alchemica.ancient_craft_success",
                                                Component.translatable(recipe.getTranslationKey()))
                                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                        false
                                );
                            }
                        } else {
                            potionResult = recipe.createResult(mass, purity);
                        }

                        // 顿悟配方
                        if (knowledge.unlockRecipe(recipe.getRecipeId())) {
                            this.level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.2f);
                            player.displayClientMessage(
                                    Component.translatable("message.ars_alchemica.eureka_discovery",
                                            Component.translatable(recipe.getTranslationKey()))
                                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                                    false
                            );
                        }
                    } else {
                        // 萃取生成常规连续动态复合灵药
                        potionResult = AlchemicalPotionItem.create(ModItems.getElixir(), currentVec, mass, purity, 1.0);
                    }
                } else {
                    // 未溶解任何以太元素，纯水返还
                    potionResult = new ItemStack(Items.POTION);
                }

                if (!player.isCreative()) {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, potionResult);
                    } else if (!player.getInventory().add(potionResult)) {
                        player.drop(potionResult, false);
                    }
                }

                setWaterLevel(this.waterLevel - 1);
                if (this.waterLevel <= 0) {
                    resetCauldron();
                }
                this.level.playSound(player, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.sidedSuccess(this.level.isClientSide());
            }
            return InteractionResult.PASS;
        }

        // 4. 手持草药材料直接右键投料
        if (this.waterLevel > 0 && absorbItemStack(held)) {
            this.level.playSound(player, this.worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.8f, 1.3f);
            return InteractionResult.sidedSuccess(this.level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    /**
     * 扫描吸收入水掉落物。
     */
    private void scanAndAbsorbDroppedItems(Level level, BlockPos pos) {
        if (this.waterLevel <= 0) return;
        AABB basinBox = new AABB(pos.getX() + 0.15, pos.getY() + 0.2, pos.getZ() + 0.15,
                pos.getX() + 0.85, pos.getY() + 0.9, pos.getZ() + 0.85);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, basinBox);
        for (ItemEntity itemEntity : items) {
            if (itemEntity.isAlive()) {
                ItemStack stack = itemEntity.getItem();
                if (absorbItemStack(stack)) {
                    level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.7f, 1.2f);
                    if (stack.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(stack);
                    }
                }
            }
        }
    }

    /**
     * 吸收具体物品堆栈并溶解。
     */
    public boolean absorbItemStack(ItemStack stack) {
        if (stack.isEmpty() || this.waterLevel <= 0) return false;

        AspectVector herbVector = null;
        if (stack.getItem() instanceof ProcessedHerbItem processed) {
            herbVector = processed.getRuntimeAspectVector(stack);
        } else if (stack.getItem() instanceof IAlchemicalHerb herb) {
            herbVector = herb.getAspectVector(IAlchemicalHerb.ProcessingForm.RAW);
        }

        if (herbVector != null && !herbVector.isEmpty()) {
            this.reactionState.addIngredient(herbVector);
            stack.shrink(1);
            setChanged();
            return true;
        }

        return false;
    }

    /**
     * 处理炼金事故。
     */
    private void handleMishap(Level level, BlockPos pos, MishapResult mishap) {
        switch (mishap) {
            case TOXIC_BOILOVER -> {
                // 沸溢有毒滞留云
                AreaEffectCloud cloud = new AreaEffectCloud(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
                cloud.setRadius(3.5f);
                cloud.setDuration(200);
                cloud.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 100, 1));
                level.addFreshEntity(cloud);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.8f);
                resetCauldron();
            }
            case ARCANE_EXPLOSION -> {
                // 以太剧烈爆炸反噬
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.5f, Level.ExplosionInteraction.BLOCK);
                resetCauldron();
            }
            case SLUDGE_COAGULATION -> {
                // 凝聚为废渣黑泥
                this.fluidColor = 0x1A1A1A;
            }
            case CRYSTALLIZE_SUCCESS -> {
                // 成功定型
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.5f);
            }
            case NONE -> {}
        }
    }

    public void setWaterLevel(int level) {
        this.waterLevel = Math.max(0, Math.min(level, MAX_WATER_LEVEL));
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.hasProperty(AlchemicalCauldronBlock.WATER_LEVEL)) {
                this.level.setBlock(this.worldPosition, state.setValue(AlchemicalCauldronBlock.WATER_LEVEL, this.waterLevel), 3);
            }
            setChanged();
        }
    }

    public void resetCauldron() {
        this.waterLevel = 0;
        this.fluidColor = DEFAULT_FLUID_COLOR;
        setChanged();
    }

    /**
     * 生成供副手《炼金手记》读取的“学者直觉”数据快照。
     */
    public ScholarIntuitionData createIntuitionSnapshot() {
        AspectVector vec = this.reactionState.getCurrentVector();
        double mass = vec.getTotalAspectMass();
        double purity = vec.calculatePurity() * (1.0 - this.reactionState.getTurbidity());

        // 计算主导元素倾向
        AlchemyAspect dominant = null;
        double maxAmount = 0.0;
        for (AlchemyAspect aspect : AlchemyAspect.VALUES) {
            double amt = vec.getAmount(aspect);
            if (amt > maxAmount) {
                maxAmount = amt;
                dominant = aspect;
            }
        }

        int totalTurns = this.reactionState.getClockwiseTurns() + this.reactionState.getCounterClockwiseTurns();
        AlchemicalRecipeStepTracker.RecipePhase phase = AlchemicalRecipeStepTracker.derivePhase(
                this.waterLevel,
                this.reactionState.getTemperature(),
                mass,
                this.reactionState.getAccumulatedInstability(),
                totalTurns
        );

        double mapX = vec.getX();
        double mapY = vec.getY();
        String nearbyRecipeKey = null;
        if (mass > 0.5) {
            java.util.Optional<AlchemicalRecipe> nearest = AlchemicalRecipeRegistry.getInstance().findNearestRecipe(mapX, mapY);
            if (nearest.isPresent() && nearest.get().distanceTo(mapX, mapY) <= 12.0) {
                nearbyRecipeKey = nearest.get().getTranslationKey();
            }
        }

        return new ScholarIntuitionData(
                this.reactionState.getTemperature(),
                this.reactionState.getActiveHeatLevel(),
                this.waterLevel,
                mass,
                purity,
                this.reactionState.getAccumulatedInstability(),
                this.reactionState.getTurbidity(),
                this.reactionState.getClockwiseTurns(),
                this.reactionState.getCounterClockwiseTurns(),
                this.fluidColor,
                dominant,
                phase.getTranslationKey(),
                mapX,
                mapY,
                nearbyRecipeKey
        );
    }

    public CauldronReactionState getReactionState() {
        return reactionState;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public int getFluidColor() {
        return fluidColor;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("WaterLevel", this.waterLevel);
        tag.putInt("FluidColor", this.fluidColor);
        tag.put("ReactionState", this.reactionState.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.waterLevel = tag.getInt("WaterLevel");
        this.fluidColor = tag.contains("FluidColor") ? tag.getInt("FluidColor") : DEFAULT_FLUID_COLOR;
        if (tag.contains("ReactionState")) {
            this.reactionState.deserializeNBT(tag.getCompound("ReactionState"));
        }
    }
}
