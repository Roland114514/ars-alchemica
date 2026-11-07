作为《Ars Alchemica》的底层架构师与应用数学家，我将继续推进 **【Milestone 2：草药系统底层】第二部分** 的核心研发。本阶段的目标是将第一部分的纯数学张量变换，具象化为高沉浸感、低性能消耗（Zero-Lag）的物理加工工坊器械与动态物品体系。

以下是符合生产级工业标准、严格遵循 **Minecraft 1.20.1 (Forge)** 规范的完整 Java 17 源码实现。

---

### 1. 统一加工产物物品：`ProcessedHerbItem.java`

为了避免 $7 \times 5 = 35$ 种物品造成的注册表膨胀与 JSON 地狱，这里采用**参数化动态渲染与 NBT 驱动架构**。同时内置客户端 `ItemColor` 着色器接口与中世纪暗黑风格的精确数学属性面板。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.core.math.AlchemyAspect;
import com.medievalmagic.arsalchemica.core.math.AspectVector;
import com.medievalmagic.arsalchemica.herb.HerbProperty;
import com.medievalmagic.arsalchemica.herb.HerbProperty.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.HerbRegistry.HerbEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * 统一草药物理加工产物载体
 * 承载 7 大基础草药在 5 种物理形态下的多态表现及动态张量解算
 */
public class ProcessedHerbItem extends Item {

    public static final String TAG_ALCHEMICAL_DATA = "AlchemicalData";
    public static final String TAG_HERB_ID = "HerbId";
    public static final String TAG_FORM = "Form";
    public static final String TAG_QUALITY = "Quality";
    public static final String TAG_IS_SLAG = "IsSlag";

    private final HerbEntry defaultHerb;
    private final ProcessingForm defaultForm;

    public ProcessedHerbItem(Properties properties, HerbEntry defaultHerb, ProcessingForm defaultForm) {
        super(properties);
        this.defaultHerb = defaultHerb;
        this.defaultForm = defaultForm;
    }

    /**
     * 工厂方法：构建带有精确药学属性与品质系数的产物实例
     */
    public static ItemStack create(Item itemInstance, HerbEntry herb, ProcessingForm form, float qualityMultiplier) {
        ItemStack stack = new ItemStack(itemInstance);
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ALCHEMICAL_DATA);
        tag.putString(TAG_HERB_ID, herb.id());
        tag.putString(TAG_FORM, form.name());
        tag.putFloat(TAG_QUALITY, Math.max(0.1f, Math.min(2.0f, qualityMultiplier)));
        tag.putBoolean(TAG_IS_SLAG, false);
        return stack;
    }

    /**
     * 工厂方法：生成过度加工或脱脂后的碳化废渣 (Charred Slag)
     */
    public static ItemStack createSlag(Item itemInstance, HerbEntry sourceHerb) {
        ItemStack stack = new ItemStack(itemInstance);
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ALCHEMICAL_DATA);
        tag.putString(TAG_HERB_ID, sourceHerb.id());
        tag.putString(TAG_FORM, ProcessingForm.MILLED_FINE.name());
        tag.putFloat(TAG_QUALITY, 0.0f);
        tag.putBoolean(TAG_IS_SLAG, true);
        return stack;
    }

    public HerbEntry getHerb(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        if (tag != null && tag.contains(TAG_HERB_ID)) {
            HerbEntry entry = HerbRegistry.getById(tag.getString(TAG_HERB_ID));
            if (entry != null) return entry;
        }
        return this.defaultHerb;
    }

    public ProcessingForm getForm(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        if (tag != null && tag.contains(TAG_FORM)) {
            try {
                return ProcessingForm.valueOf(tag.getString(TAG_FORM));
            } catch (IllegalArgumentException ignored) {}
        }
        return this.defaultForm;
    }

    public float getQuality(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        return (tag != null && tag.contains(TAG_QUALITY)) ? tag.getFloat(TAG_QUALITY) : 1.0f;
    }

    public boolean isSlag(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        return tag != null && tag.getBoolean(TAG_IS_SLAG);
    }

    /**
     * 实时解算当前物品栈的药理属性张量 (结合加工形态矩阵与品质标量)
     */
    public HerbProperty computeRuntimeProperty(ItemStack stack) {
        HerbEntry herb = getHerb(stack);
        if (isSlag(stack)) {
            // 废渣仅残留极微量的死寂(MORTIS)与地脉(TERRA)及高额毒性
            AspectVector slagVec = AspectVector.builder()
                    .set(AlchemyAspect.TERRA, 0.5f)
                    .set(AlchemyAspect.MORTIS, 0.8f)
                    .build();
            return new HerbProperty(slagVec, 0.05f, 0.0f, 1.5f, ProcessingForm.MILLED_COARSE);
        }
        ProcessingForm form = getForm(stack);
        float quality = getQuality(stack);
        HerbProperty transformed = herb.baseProperty().transformTo(form);
        return new HerbProperty(
                transformed.baseAspects().scale(quality),
                Math.min(1.0f, transformed.purity() * quality),
                transformed.volatility(),
                transformed.toxicity(),
                form
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isSlag(stack)) {
            return Component.translatable("item.arsalchemica.charred_slag")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }
        HerbEntry herb = getHerb(stack);
        ProcessingForm form = getForm(stack);
        return Component.translatable("item.arsalchemica.herb_form." + form.name().toLowerCase(Locale.ROOT),
                Component.translatable("herb.arsalchemica." + herb.id()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isSlag(stack)) {
            tooltip.add(Component.literal("❝ 药性已在粗暴的物理机械力下彻底碳化崩解。 ❞")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("☠ 残留毒性: 极高 (不可入药)").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        HerbProperty prop = computeRuntimeProperty(stack);
        ProcessingForm form = prop.form();

        // 1. 形态与拉丁学名引文
        tooltip.add(Component.translatable("tooltip.arsalchemica.form." + form.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

        // 2. 纯度星级解算 (5星制，带半星精度)
        tooltip.add(buildPurityStars(prop.purity()));

        // 3. 物理动力学指标：挥发性与毒性
        MutableComponent kinetics = Component.literal("❖ 挥发率: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("%.1f%%", prop.volatility() * 100f)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  ☠ 毒性指数: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format("%.2f", prop.toxicity()))
                        .withStyle(prop.toxicity() > 0.5f ? ChatFormatting.RED : ChatFormatting.GREEN));
        tooltip.add(kinetics);

        // 4. 分割线
        tooltip.add(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        // 5. 主导元素图标与张量模长渲染
        AspectVector vec = prop.baseAspects();
        for (AlchemyAspect aspect : AlchemyAspect.VALUES) {
            float val = vec.get(aspect);
            if (val > 0.05f) {
                tooltip.add(formatAspectLine(aspect, val));
            }
        }
    }

    private Component buildPurityStars(float purity) {
        int fullStars = (int) (purity * 5.0f);
        boolean halfStar = (purity * 5.0f - fullStars) >= 0.5f;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) sb.append("★");
            else if (i == fullStars && halfStar) sb.append("✪");
            else sb.append("☆");
        }
        ChatFormatting starColor = purity >= 0.85f ? ChatFormatting.LIGHT_PURPLE
                : (purity >= 0.6f ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

        return Component.literal("✦ 灵质纯度: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sb.toString()).withStyle(starColor))
                .append(Component.literal(String.format(" (%.0f%%)", purity * 100f)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private Component formatAspectLine(AlchemyAspect aspect, float value) {
        String icon = switch (aspect) {
            case IGNIS -> "🜂";
            case AQUA -> "🜄";
            case TERRA -> "🜃";
            case AER -> "🜁";
            case LUX -> "☼";
            case UMBRA -> "☾";
            case ORDO -> "⚖";
            case CHAOS -> "❈";
            case VITA -> "⚕";
            case MORTIS -> "☠";
            case AETHER -> "✧";
            case INFERNUS -> "⛧";
        };
        Style colorStyle = Style.EMPTY.withColor(aspect.getColor());
        return Component.literal("  " + icon + " ").setStyle(colorStyle)
                .append(Component.translatable("aspect.arsalchemica." + aspect.name().toLowerCase(Locale.ROOT)).setStyle(colorStyle))
                .append(Component.literal(String.format(" : %.2f", value)).withStyle(ChatFormatting.GRAY));
    }

    /**
     * 32x32 物品贴图双层动态着色器 (注册于 RegisterColorHandlersEvent.Item)
     * Layer 0: 基底容器或叶脉明暗纹理 (根据加工形态发生色度偏移)
     * Layer 1: 高光精华粉尘或液面反光层 (取主导炼金元素的颜色)
     */
    @OnlyIn(Dist.CLIENT)
    public static class DynamicHerbItemColor implements ItemColor {
        @Override
        public int getColor(ItemStack stack, int tintIndex) {
            if (!(stack.getItem() instanceof ProcessedHerbItem item)) return 0xFFFFFFFF;
            if (item.isSlag(stack)) return 0xFF2B2826; // 碳化焦黑

            HerbEntry herb = item.getHerb(stack);
            ProcessingForm form = item.getForm(stack);
            int baseColor = herb.tintColor();

            if (tintIndex == 0) {
                return switch (form) {
                    case RAW -> baseColor;
                    case MILLED_COARSE -> blendColor(baseColor, 0xFF8C8275, 0.25f); // 掺杂石臼灰感
                    case MILLED_FINE -> blendColor(baseColor, 0xFFEFECE6, 0.35f);   // 微粉散射增亮
                    case PRESSED_OIL -> 0xFFFFFFFF;                                 // Layer 0 为透明玻璃瓶本身，不染色
                    case DRIED -> darkenAndDesaturate(baseColor, 0.65f);            // 脱水枯萎暗化
                };
            } else if (tintIndex == 1) {
                HerbProperty prop = item.computeRuntimeProperty(stack);
                AlchemyAspect dominant = prop.baseAspects().getDominantAspect();
                int aspectColor = dominant != null ? dominant.getColor() : baseColor;
                return form == ProcessingForm.PRESSED_OIL ? blendColor(baseColor, aspectColor, 0.5f) : aspectColor;
            }
            return 0xFFFFFFFF;
        }

        private static int blendColor(int c1, int c2, float ratio) {
            int r = (int) (((c1 >> 16) & 0xFF) * (1 - ratio) + ((c2 >> 16) & 0xFF) * ratio);
            int g = (int) (((c1 >> 8) & 0xFF) * (1 - ratio) + ((c2 >> 8) & 0xFF) * ratio);
            int b = (int) ((c1 & 0xFF) * (1 - ratio) + (c2 & 0xFF) * ratio);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        private static int darkenAndDesaturate(int color, float factor) {
            int r = (int) (((color >> 16) & 0xFF) * factor);
            int g = (int) (((color >> 8) & 0xFF) * factor);
            int b = (int) ((color & 0xFF) * factor);
            int gray = (r + g + b) / 3;
            r = (r + gray) / 2;
            g = (g + gray) / 2;
            b = (b + gray) / 2;
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
}
```

---

### 2. 手摇研磨钵：`MortarBlock.java` & `MortarBlockEntity.java`

#### `MortarBlock.java`
支持空手投入/取出、持杵（通过物品 Tag 或类名判定）研磨，零 Tick 消耗，完全由玩家交互事件驱动。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 黑曜石镶边研磨石臼方块
 * 完全事件驱动 (Zero-Tick Overhead)
 */
public class MortarBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 7.0D, 13.0D);
    public static final TagKey<Item> PESTLE_TAG = ItemTags.create(new ResourceLocation("arsalchemica", "pestles"));

    public MortarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity mortar)) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);

        // 1. 手持研磨杵 (Pestle)：执行机械研磨状态机演进
        if (isPestle(heldStack)) {
            if (!mortar.isEmpty()) {
                if (!level.isClientSide) {
                    mortar.performGrind(player, heldStack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.CONSUME;
        }

        // 2. 空手交互：取出当前石臼内的物品
        if (heldStack.isEmpty()) {
            if (!mortar.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack extracted = mortar.extractItem();
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 3. 持有可研磨草药：放入石臼
        if (mortar.isEmpty() && mortar.canAccept(heldStack)) {
            if (!level.isClientSide) {
                ItemStack toInsert = heldStack.split(1);
                mortar.insertItem(toInsert);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private boolean isPestle(ItemStack stack) {
        return stack.is(PESTLE_TAG) || stack.getItem().getDescriptionId().contains("pestle");
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof MortarBlockEntity mortar && !mortar.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), mortar.extractItem());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 生产环境中替换为 ModBlockEntities.MORTAR.get()
        return new MortarBlockEntity(null, pos, state);
    }
}
```

#### `MortarBlockEntity.java`
实现严谨的分段研磨状态机（1~3粗粉，4~6微粉，>8碳化损毁）与粒子、音效物理反馈。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbProperty.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.HerbRegistry.HerbEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 手摇研磨钵实体与离散状态机
 */
public class MortarBlockEntity extends BlockEntity {

    private ItemStack content = ItemStack.EMPTY;
    private int grindCount = 0;
    private long lastGrindTick = 0L;

    public MortarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean canAccept(ItemStack stack) {
        if (stack.getItem() instanceof ProcessedHerbItem herbItem) {
            if (herbItem.isSlag(stack)) return false;
            ProcessingForm form = herbItem.getForm(stack);
            return form == ProcessingForm.RAW || form == ProcessingForm.DRIED || form == ProcessingForm.MILLED_COARSE;
        }
        return false;
    }

    public void insertItem(ItemStack stack) {
        this.content = stack.copyWithCount(1);
        if (stack.getItem() instanceof ProcessedHerbItem herbItem) {
            ProcessingForm form = herbItem.getForm(stack);
            // 若放入的已是粗粉，则状态机直接从第3次研磨起步
            this.grindCount = (form == ProcessingForm.MILLED_COARSE) ? 3 : 0;
        } else {
            this.grindCount = 0;
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        sync();
    }

    public ItemStack extractItem() {
        ItemStack extracted = this.content;
        this.content = ItemStack.EMPTY;
        this.grindCount = 0;
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        sync();
        return extracted;
    }

    /**
     * 核心研磨状态机：
     * - 1 ~ 3 次：产出粗粉 (MILLED_COARSE)
     * - 4 ~ 6 次：产出超细微粉 (MILLED_FINE)，并在第6次达到纯度峰值
     * - 7 ~ 8 次：过热警告期（冒出零星黑烟，纯度下滑）
     * - > 8 次  ：药性彻底碳化损毁，变成药渣 (Charred Slag)
     */
    public void performGrind(Player player, ItemStack pestle) {
        if (this.level == null || this.level.isClientSide || isEmpty()) return;

        long gameTime = this.level.getGameTime();
        if (gameTime - this.lastGrindTick < 6L) return; // 防宏连点节流 (0.3s 物理冷却)
        this.lastGrindTick = gameTime;

        if (!(this.content.getItem() instanceof ProcessedHerbItem herbItem)) return;

        this.grindCount++;
        HerbEntry herb = herbItem.getHerb(this.content);
        float baseQuality = herbItem.getQuality(this.content);

        ServerLevel serverLevel = (ServerLevel) this.level;
        double px = this.worldPosition.getX() + 0.5D;
        double py = this.worldPosition.getY() + 0.35D;
        double pz = this.worldPosition.getZ() + 0.5D;

        // 播放研杵碾压碎石与摩擦音效
        float pitch = 0.85f + (this.grindCount * 0.05f);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, pitch);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.TUFF_BREAK, SoundSource.BLOCKS, 0.8f, 0.7f);

        // 飞溅草药碎屑粒子
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.content),
                px, py, pz, 8, 0.12D, 0.08D, 0.12D, 0.03D);

        // 状态机跃迁解算
        if (this.grindCount >= 1 && this.grindCount <= 3) {
            float progressBonus = 0.90f + (this.grindCount * 0.04f); // 0.94 -> 0.98 -> 1.02
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_COARSE, baseQuality * progressBonus);

        } else if (this.grindCount >= 4 && this.grindCount <= 6) {
            float fineBonus = 1.0f + ((this.grindCount - 4) * 0.05f); // 第6次达到 1.10x 最佳研磨品质
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_FINE, baseQuality * fineBonus);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py + 0.1D, pz, 2, 0.1D, 0.05D, 0.1D, 0.0D);

        } else if (this.grindCount == 7 || this.grindCount == 8) {
            // 机械过热摩擦警告：品质衰减并伴随焦糊烟雾
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_FINE, baseQuality * 0.75f);
            serverLevel.sendParticles(ParticleTypes.SMOKE, px, py + 0.1D, pz, 6, 0.08D, 0.05D, 0.08D, 0.01D);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.6f);

        } else if (this.grindCount > 8) {
            // 过度研磨 (>8次)：药性碳化崩解为废渣
            if (!herbItem.isSlag(this.content)) {
                this.content = ProcessedHerbItem.createSlag(herbItem, herb);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py + 0.1D, pz, 15, 0.15D, 0.1D, 0.15D, 0.02D);
                serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 3, 0.05D, 0.05D, 0.05D, 0.0D);
                serverLevel.playSound(null, this.worldPosition, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 0.9f, 0.8f);
            }
        }

        // 消耗杵耐久度
        if ( pestle.isDamageableItem()) {
            pestle.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }

        sync();
    }

    public ItemStack getRenderStack() {
        return this.content;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Content", this.content.save(new CompoundTag()));
        tag.putInt("GrindCount", this.grindCount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.content = ItemStack.of(tag.getCompound("Content"));
        this.grindCount = tag.getInt("GrindCount");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

---

### 3. 杠杆压榨台：`HerbPressBlock.java` & `HerbPressBlockEntity.java`

#### `HerbPressBlock.java`
玩家可向上槽位放入新鲜草药 (`RAW`)，向下槽位放入空玻璃瓶 (`Items.GLASS_BOTTLE`)，空手右键拉动杠杆活塞完成冷压萃取。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 重型螺旋杠杆压榨台方块
 */
public class HerbPressBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public HerbPressBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof HerbPressBlockEntity press)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. 放入空玻璃瓶或药剂小瓶至底座接液槽
        if (held.is(Items.GLASS_BOTTLE) && press.canInsertVial()) {
            if (!level.isClientSide) {
                press.insertVial(held.split(1));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 放入新鲜草药 (RAW) 至压榨膛室
        if (press.canInsertHerb(held)) {
            if (!level.isClientSide) {
                press.insertHerb(held.split(1));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. 空手交互：优先取出已榨取的精油瓶；若处于待压榨状态，则拉动杠杆下压活塞！
        if (held.isEmpty()) {
            if (press.hasOutputOil()) {
                if (!level.isClientSide) {
                    ItemStack oil = press.extractVialSlot();
                    if (!player.getInventory().add(oil)) player.drop(oil, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (press.canActivatePress()) {
                if (!level.isClientSide) {
                    press.activateLeverPress(state);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // 若条件不满足但槽内有物品，则允许空手退回原材料
            if (!level.isClientSide) {
                ItemStack returned = press.extractAnyRemaining();
                if (!returned.isEmpty() && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof HerbPressBlockEntity press) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), press.getHerbSlot());
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), press.getVialSlot());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HerbPressBlockEntity(null, pos, state);
    }
}
```

#### `HerbPressBlockEntity.java`
每次右键扳动杠杆推进 `pressStage`（共需2次重压），满载时将草药转化为浓缩精油 (`PRESSED_OIL`) 注入下方小瓶，同时向侧面弹出脱脂废渣。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbProperty.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.HerbRegistry.HerbEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 杠杆压榨台实体：控制机械增压、精油萃取与废渣弹出
 */
public class HerbPressBlockEntity extends BlockEntity {

    private ItemStack herbSlot = ItemStack.EMPTY;
    private ItemStack vialSlot = ItemStack.EMPTY;
    private int pressStage = 0; // 0: 未下压, 1: 半压破壁, 2: 完全压榨萃取

    public HerbPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean canInsertVial() {
        return this.vialSlot.isEmpty();
    }

    public void insertVial(ItemStack stack) {
        this.vialSlot = stack.copyWithCount(1);
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.7f, 1.3f);
        }
        sync();
    }

    public boolean canInsertHerb(ItemStack stack) {
        if (!this.herbSlot.isEmpty()) return false;
        if (stack.getItem() instanceof ProcessedHerbItem herbItem && !herbItem.isSlag(stack)) {
            // 仅允许饱含汁液的新鲜草药 (RAW) 进行压榨
            return herbItem.getForm(stack) == ProcessingForm.RAW;
        }
        return false;
    }

    public void insertHerb(ItemStack stack) {
        this.herbSlot = stack.copyWithCount(1);
        this.pressStage = 0;
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.AZALEA_PLACE, SoundSource.BLOCKS, 0.9f, 0.9f);
            this.level.setBlock(this.worldPosition, getBlockState().setValue(HerbPressBlock.PRESSED, false), Block.UPDATE_ALL);
        }
        sync();
    }

    public boolean hasOutputOil() {
        return !this.vialSlot.isEmpty() && this.vialSlot.getItem() instanceof ProcessedHerbItem;
    }

    public boolean canActivatePress() {
        return !this.herbSlot.isEmpty() && this.vialSlot.is(Items.GLASS_BOTTLE);
    }

    /**
     * 激活杠杆机械下压
     */
    public void activateLeverPress(BlockState state) {
        if (this.level == null || this.level.isClientSide || !canActivatePress()) return;
        if (!(this.herbSlot.getItem() instanceof ProcessedHerbItem herbItem)) return;

        ServerLevel serverLevel = (ServerLevel) this.level;
        this.pressStage++;

        double px = this.worldPosition.getX() + 0.5D;
        double py = this.worldPosition.getY() + 0.45D;
        double pz = this.worldPosition.getZ() + 0.5D;

        if (this.pressStage == 1) {
            // 第一段下压：木质齿轮咬合与植物细胞壁破裂声
            serverLevel.playSound(null, this.worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 0.6f);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, px, py, pz, 6, 0.15D, 0.05D, 0.15D, 0.0D);
        } else if (this.pressStage >= 2) {
            // 第二段满负荷下压：完成浓缩精油 (PRESSED_OIL) 提取！
            HerbEntry herb = herbItem.getHerb(this.herbSlot);
            float quality = herbItem.getQuality(this.herbSlot);

            // 1. 将下方空玻璃瓶替换为高纯度浓缩精油瓶
            this.vialSlot = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.PRESSED_OIL, quality);

            // 2. 生成脱脂残渣并从排渣口物理弹出
            ItemStack defattedSlag = ProcessedHerbItem.createSlag(herbItem, herb);
            this.herbSlot = ItemStack.EMPTY;
            this.pressStage = 0;

            Containers.dropItemStack(serverLevel, px, py + 0.25D, pz, defattedSlag);

            // 3. 更新活塞方块状态并提供声光反馈
            serverLevel.setBlock(this.worldPosition, state.setValue(HerbPressBlock.PRESSED, true), Block.UPDATE_ALL);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.25f, 0.5f);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.4f);
            serverLevel.sendParticles(ParticleTypes.WITCH, px, py, pz, 12, 0.2D, 0.1D, 0.2D, 0.02D);
        }

        sync();
    }

    public ItemStack extractVialSlot() {
        ItemStack out = this.vialSlot;
        this.vialSlot = ItemStack.EMPTY;
        if (this.level != null) {
            this.level.setBlock(this.worldPosition, getBlockState().setValue(HerbPressBlock.PRESSED, false), Block.UPDATE_ALL);
            this.level.playSound(null, this.worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        sync();
        return out;
    }

    public ItemStack extractAnyRemaining() {
        if (!this.herbSlot.isEmpty()) {
            ItemStack out = this.herbSlot;
            this.herbSlot = ItemStack.EMPTY;
            this.pressStage = 0;
            sync();
            return out;
        }
        if (!this.vialSlot.isEmpty()) {
            return extractVialSlot();
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getHerbSlot() { return this.herbSlot; }
    public ItemStack getVialSlot() { return this.vialSlot; }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("HerbSlot", this.herbSlot.save(new CompoundTag()));
        tag.put("VialSlot", this.vialSlot.save(new CompoundTag()));
        tag.putInt("PressStage", this.pressStage);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.herbSlot = ItemStack.of(tag.getCompound("HerbSlot"));
        this.vialSlot = ItemStack.of(tag.getCompound("VialSlot"));
        this.pressStage = tag.getInt("PressStage");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

---

### 4. 通风风干架：`DryingRackBlock.java` & `DryingRackBlockEntity.java`

#### `DryingRackBlock.java`
可挂载在墙面或悬吊于天花板横梁下，最多同时悬挂 4 株草药。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 杉木通风风干架方块 (支持墙面挂载与横梁悬吊)
 */
public class DryingRackBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CEILING_MOUNTED = BooleanProperty.create("ceiling");

    private static final VoxelShape SHAPE_CEILING = Block.box(2.0D, 10.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_NORTH = Block.box(1.0D, 8.0D, 11.0D, 15.0D, 15.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0D, 8.0D, 0.0D, 15.0D, 15.0D, 5.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(11.0D, 8.0D, 1.0D, 16.0D, 15.0D, 15.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 8.0D, 1.0D, 5.0D, 15.0D, 15.0D);

    public DryingRackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CEILING_MOUNTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CEILING_MOUNTED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(CEILING_MOUNTED, true);
        } else if (clickedFace.getAxis().isHorizontal()) {
            return this.defaultBlockState()
                    .setValue(FACING, clickedFace)
                    .setValue(CEILING_MOUNTED, false);
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CEILING_MOUNTED, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CEILING_MOUNTED)) return SHAPE_CEILING;
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_CEILING;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. 悬挂新鲜草药
        if (rack.canHangHerb(held)) {
            if (!level.isClientSide && rack.hangHerb(held.split(1))) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 空手取下草药 (优先取下已完成阴干的草药)
        if (held.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack retrieved = rack.takeHerb();
                if (!retrieved.isEmpty()) {
                    if (!player.getInventory().add(retrieved)) player.drop(retrieved, false);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
                for (ItemStack stack : rack.getSlots()) {
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(null, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 客户端零 Tick 消耗，仅在服务端挂载相位惰性 Ticker
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof DryingRackBlockEntity rack) {
                DryingRackBlockEntity.serverTick(lvl, pos, st, rack);
            }
        };
    }
}
```

#### `DryingRackBlockEntity.java`
严格遵循 **Zero-Lag 相位惰性 Tick 铁律**：利用坐标哈希打散相位 `(gameTime + posHash) % 20 == 0`，将全服上千个风干架的运算均匀摊平到 20 个 Tick 中，每秒仅执行 1 次热力学与通风度解算。

```java
package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbProperty.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.HerbRegistry.HerbEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 通风风干架方块实体
 * 遵循 Zero-Lag 相位分摊惰性 Tick 铁律，模拟真实温湿度与气流脱水动力学
 */
public class DryingRackBlockEntity extends BlockEntity {

    public static final int SLOT_COUNT = 4;
    // 标准环境下需 120 秒 (即 120 次惰性相位更新) 完成自然脱水阴干
    private static final float TARGET_DRYING_UNITS = 120.0f;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final float[] dryingProgress = new float[SLOT_COUNT];
    private final int phaseOffset;
    private boolean hasActiveRawHerbs = false;

    public DryingRackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 利用斐波那契空间哈希确定该坐标专属的 Tick 相位偏移 [0, 19]
        this.phaseOffset = Math.floorMod(Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()), 20);
    }

    /**
     * Zero-Lag 相位惰性 Tick：
     * 1. 若无需要风干的新鲜草药，100% 瞬间短路退出 (O(1) 零开销)
     * 2. 仅在匹配的相位帧 (每 20 Tick 触发 1 次) 进行环境温度与通风采样
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        if (!rack.hasActiveRawHerbs) return;
        if ((level.getGameTime() + rack.phaseOffset) % 20L != 0L) return;

        // 雨天淋雨或高湿露天环境：暂停脱水计时
        if (level.isRainingAt(pos) || (level.isRaining() && level.canSeeSky(pos))) {
            return;
        }

        float envRate = rack.computeThermodynamicRate(level, pos);
        if (envRate <= 0.0f) return;

        boolean stateChanged = false;
        boolean stillHasRaw = false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = rack.slots.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ProcessedHerbItem herbItem)) continue;

            if (herbItem.getForm(stack) == ProcessingForm.RAW && !herbItem.isSlag(stack)) {
                rack.dryingProgress[i] += envRate;
                if (rack.dryingProgress[i] >= TARGET_DRYING_UNITS) {
                    // 脱水完成，转化为阴干形态 (DRIED)
                    HerbEntry herb = herbItem.getHerb(stack);
                    float quality = herbItem.getQuality(stack);
                    rack.slots.set(i, ProcessedHerbItem.create(herbItem, herb, ProcessingForm.DRIED, quality));
                    rack.dryingProgress[i] = 0.0f;
                    stateChanged = true;
                } else {
                    stillHasRaw = true;
                }
            }
        }

        rack.hasActiveRawHerbs = stillHasRaw;
        if (stateChanged) {
            level.playSound(null, pos, SoundEvents.VINE_STEP, SoundSource.BLOCKS, 0.5f, 1.4f);
            rack.sync();
        } else {
            rack.setChanged();
        }
    }

    /**
     * 解算局部微气候脱水速率：
     * Rate = Base(1.0) * TempFactor * VentilationFactor
     */
    private float computeThermodynamicRate(Level level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        float baseTemp = biome.getBaseTemperature(); // 沙漠 2.0, 平原 0.8, 雪原 0.0
        float tempFactor = Mth.clamp(0.4f + (baseTemp * 0.6f), 0.25f, 2.0f);

        // 采样六向邻接空气方块以评估通风率
        int openAirFaces = 0;
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).isAir()) {
                openAirFaces++;
            }
        }
        float ventilationFactor = 0.4f + (openAirFaces * 0.15f); // [0.4, 1.3]

        // 若处于阴暗通风的室内（非烈日暴晒），额外获得阴干药效保护加速
        boolean isShaded = !level.canSeeSky(pos);
        float shadeMultiplier = isShaded ? 1.15f : 0.95f;

        return tempFactor * ventilationFactor * shadeMultiplier;
    }

    public boolean canHangHerb(ItemStack stack) {
        if (!(stack.getItem() instanceof ProcessedHerbItem herbItem) || herbItem.isSlag(stack)) {
            return false;
        }
        ProcessingForm form = herbItem.getForm(stack);
        if (form != ProcessingForm.RAW && form != ProcessingForm.DRIED) return false;

        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.slots.get(i).isEmpty()) return true;
        }
        return false;
    }

    public boolean hangHerb(ItemStack stack) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.slots.get(i).isEmpty()) {
                this.slots.set(i, stack.copyWithCount(1));
                this.dryingProgress[i] = 0.0f;
                recalculateActiveFlag();
                if (this.level != null) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
                }
                sync();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeHerb() {
        // 优先取下已阴干完毕的草药
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            ItemStack s = this.slots.get(i);
            if (!s.isEmpty() && s.getItem() instanceof ProcessedHerbItem herbItem
                    && herbItem.getForm(s) == ProcessingForm.DRIED) {
                return removeSlot(i);
            }
        }
        // 其次按倒序取下任意挂载的草药
        for (int i = SLOT_COUNT - 1; i >= 0; i--) {
            if (!this.slots.get(i).isEmpty()) {
                return removeSlot(i);
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack removeSlot(int index) {
        ItemStack out = this.slots.get(index);
        this.slots.set(index, ItemStack.EMPTY);
        this.dryingProgress[index] = 0.0f;
        recalculateActiveFlag();
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        sync();
        return out;
    }

    private void recalculateActiveFlag() {
        this.hasActiveRawHerbs = false;
        for (ItemStack s : this.slots) {
            if (!s.isEmpty() && s.getItem() instanceof ProcessedHerbItem herbItem
                    && herbItem.getForm(s) == ProcessingForm.RAW && !herbItem.isSlag(s)) {
                this.hasActiveRawHerbs = true;
                break;
            }
        }
    }

    public NonNullList<ItemStack> getSlots() {
        return this.slots;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            CompoundTag slotTag = new CompoundTag();
            this.slots.get(i).save(slotTag);
            slotTag.putFloat("Progress", this.dryingProgress[i]);
            list.add(slotTag);
        }
        tag.put("RackSlots", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ListTag list = tag.getList("RackSlots", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(SLOT_COUNT, list.size()); i++) {
            CompoundTag slotTag = list.getCompound(i);
            this.slots.set(i, ItemStack.of(slotTag));
            this.dryingProgress[i] = slotTag.getFloat("Progress");
        }
        recalculateActiveFlag();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

---

### 架构师技术审阅备忘录 (Architecture Review Notes)

1. **线性代数与物理加工的无缝闭环**：
   `ProcessedHerbItem#computeRuntimeProperty` 直接调用了我们在 Milestone 2 第一部分编写的 `HerbProperty#transformTo(ProcessingForm)`，使得研磨钵、压榨台和风干架产出的物品无需硬编码任何元素数值，全部通过高维张量旋转与耗散矩阵自动推导。
2. **极致的服务器 TPS 保护（Zero-Lag 铁律）**：
   - `MortarBlockEntity` 与 `HerbPressBlockEntity` **未注册任何 `BlockEntityTicker`**，平时处于完全休眠态（0 CPU 占用），仅在玩家右键点击的瞬间触发离散状态机。
   - `DryingRackBlockEntity` 采用 `hasActiveRawHerbs` 布尔门控 + 基于斐波那契坐标哈希 `Mth.getSeed(x, y, z)` 的 20-Tick 相位分摊算法，即使在大型中世纪工坊中密集悬挂 1,000 个风干架，每 Tick 也仅有最多 50 个实例进行轻量级浮点数加法，且仅在完成阴干的那一帧才触发一次网络同步包（`sendBlockUpdated`）。