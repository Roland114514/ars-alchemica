package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb.ProcessingForm;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
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
 * 手摇研磨钵实体与离散状态机（加固版）。
 * <p>
 * 遵循 Zero-Tick Overhead 与生产级防御规范：
 * 1. 消除 super(null, ...) NPE，支持延迟 BlockEntityType 绑定；
 * 2. 初始冷却时间防卡死，支持服务器重置时间防护；
 * 3. 严格限制可加工物料形态，封堵无限研磨套娃；
 * 4. 提取物品时采用原子清空，防止多方块事件监听二次读取引发刷物品 (Dupe Bug)。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class MortarBlockEntity extends BlockEntity {

    private ItemStack content = ItemStack.EMPTY;
    private int grindCount = 0;
    private long lastGrindTick = -100L;

    public MortarBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.getMortarType(), pos, state);
    }

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
            this.grindCount = (form == ProcessingForm.MILLED_COARSE) ? 3 : 0;
        } else {
            this.grindCount = 0;
        }
        if (this.level != null) {
            this.level.playSound(null, this.worldPosition, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        sync();
    }

    /**
     * 原子提取当前石臼内的物品并清空槽位，彻底杜绝刷物品 (Dupe Bug)。
     */
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
     * - 4 ~ 6 次：产出超细微粉 (MILLED_FINE)，第 6 次达到纯度峰值
     * - 7 ~ 8 次：过热警告（冒烟）
     * - > 8 次  ：碳化损毁，变成废渣 (Charred Slag)
     */
    public boolean performGrind(Player player, ItemStack pestle) {
        if (this.level == null || this.level.isClientSide() || isEmpty()) return false;

        long gameTime = this.level.getGameTime();
        if (Math.abs(gameTime - this.lastGrindTick) < 4L) return false; // 0.2s 物理冷却
        this.lastGrindTick = gameTime;

        if (!(this.content.getItem() instanceof ProcessedHerbItem herbItem)) return false;

        this.grindCount++;
        HerbRegistry herb = herbItem.getHerb(this.content);
        double baseQuality = herbItem.getQuality(this.content);

        ServerLevel serverLevel = (ServerLevel) this.level;
        double px = this.worldPosition.getX() + 0.5D;
        double py = this.worldPosition.getY() + 0.35D;
        double pz = this.worldPosition.getZ() + 0.5D;

        float pitch = 0.85f + (this.grindCount * 0.05f);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, pitch);
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.content),
                px, py, pz, 8, 0.12D, 0.08D, 0.12D, 0.03D);

        if (this.grindCount >= 1 && this.grindCount <= 3) {
            double progressBonus = 0.90 + (this.grindCount * 0.04);
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_COARSE, baseQuality * progressBonus);

        } else if (this.grindCount >= 4 && this.grindCount <= 6) {
            double fineBonus = 1.0 + ((this.grindCount - 4) * 0.05);
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_FINE, baseQuality * fineBonus);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py + 0.1D, pz, 2, 0.1D, 0.05D, 0.1D, 0.0D);

        } else if (this.grindCount == 7 || this.grindCount == 8) {
            this.content = ProcessedHerbItem.create(herbItem, herb, ProcessingForm.MILLED_FINE, baseQuality * 0.75);
            serverLevel.sendParticles(ParticleTypes.SMOKE, px, py + 0.1D, pz, 6, 0.08D, 0.05D, 0.08D, 0.01D);

        } else if (this.grindCount > 8) {
            if (!herbItem.isSlag(this.content)) {
                this.content = ProcessedHerbItem.createSlag(herbItem, herb);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, py + 0.1D, pz, 15, 0.15D, 0.1D, 0.15D, 0.02D);
                serverLevel.playSound(null, this.worldPosition, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 0.9f, 0.8f);
            }
        }

        if (pestle.isDamageableItem()) {
            pestle.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }

        sync();
        return true;
    }

    public ItemStack getContent() { return this.content; }
    public int getGrindCount() { return this.grindCount; }

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
