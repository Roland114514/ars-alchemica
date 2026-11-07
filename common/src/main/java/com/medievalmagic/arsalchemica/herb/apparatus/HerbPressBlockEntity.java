package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.item.AlchemicalHerbItem;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.world.item.Item;
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
 * 杠杆压榨台实体：控制机械增压、精油萃取与废渣弹出（加固版）。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class HerbPressBlockEntity extends BlockEntity {

    private ItemStack herbSlot = ItemStack.EMPTY;
    private ItemStack vialSlot = ItemStack.EMPTY;
    private int pressStage = 0; // 0: 未压, 1: 半压破壁, 2: 满负荷完成萃取

    public HerbPressBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.getHerbPressType(), pos, state);
    }

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
            return herbItem.getForm(stack) == ProcessingForm.RAW;
        }
        if (stack.getItem() instanceof AlchemicalHerbItem) {
            return true;
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

    public void activateLeverPress(BlockState state) {
        if (this.level == null || this.level.isClientSide() || !canActivatePress()) return;

        HerbRegistry herb;
        double quality;
        Item targetItem;
        if (this.herbSlot.getItem() instanceof ProcessedHerbItem herbItem) {
            herb = herbItem.getHerb(this.herbSlot);
            quality = herbItem.getQuality(this.herbSlot);
            targetItem = herbItem;
        } else if (this.herbSlot.getItem() instanceof AlchemicalHerbItem herbBlockItem) {
            herb = herbBlockItem.getHerb();
            quality = 1.0;
            targetItem = ModItems.getProcessedHerb() != null ? ModItems.getProcessedHerb() : this.herbSlot.getItem();
        } else {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level;
        this.pressStage++;

        double px = this.worldPosition.getX() + 0.5D;
        double py = this.worldPosition.getY() + 0.45D;
        double pz = this.worldPosition.getZ() + 0.5D;

        if (this.pressStage == 1) {
            serverLevel.playSound(null, this.worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.7f, 0.6f);
            serverLevel.playSound(null, this.worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6f, 0.8f);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, px, py, pz, 6, 0.15D, 0.05D, 0.15D, 0.0D);
        } else if (this.pressStage >= 2) {
            // 转化为浓缩精油瓶（继承原生草药品质）
            this.vialSlot = ProcessedHerbItem.create(targetItem, herb, ProcessingForm.PRESSED_OIL, quality);

            // 弹出残渣
            ItemStack defattedSlag = ProcessedHerbItem.createSlag(targetItem, herb);
            this.herbSlot = ItemStack.EMPTY;
            this.pressStage = 0;

            Containers.dropItemStack(serverLevel, px, py + 0.25D, pz, defattedSlag);

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
    public int getPressStage() { return this.pressStage; }

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
