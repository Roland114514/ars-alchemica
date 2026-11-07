package com.medievalmagic.arsalchemica.cauldron;

import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 浸入式多态温控大坩埚方块 (Alchemical Cauldron Block)。
 * <p>
 * 具备双状态机属性：
 * <ul>
 *   <li>{@link #WATER_LEVEL} (0 ~ 3)：当前坩埚内的液面高度刻度；</li>
 *   <li>{@link #HEAT_TIER} (0 ~ 3)：当前受热能级（0=室温, 1=文火慢炖, 2=剧烈沸腾, 3=星界极炎）。</li>
 * </ul>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalCauldronBlock extends BaseEntityBlock {

    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 3);
    public static final IntegerProperty HEAT_TIER = IntegerProperty.create("heat_tier", 0, 3);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),   // 底座
            Block.box(0.0, 3.0, 0.0, 16.0, 16.0, 16.0)   // 主体锅身
    );

    public AlchemicalCauldronBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(WATER_LEVEL, 0)
                .setValue(HEAT_TIER, 0));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemicalCauldronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.getCauldronType(), AlchemicalCauldronBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AlchemicalCauldronBlockEntity cauldron) {
            return cauldron.onPlayerInteract(player, hand, hit);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof ItemEntity itemEntity && itemEntity.isAlive()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AlchemicalCauldronBlockEntity cauldron && cauldron.getWaterLevel() > 0) {
                ItemStack stack = itemEntity.getItem();
                if (cauldron.absorbItemStack(stack)) {
                    if (stack.isEmpty()) {
                        itemEntity.discard();
                    } else {
                        itemEntity.setItem(stack);
                    }
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER_LEVEL, HEAT_TIER);
    }
}
