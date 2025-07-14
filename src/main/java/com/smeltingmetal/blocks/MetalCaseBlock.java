package com.smeltingmetal.blocks;

import com.smeltingmetal.ModBlockEntities;
import com.smeltingmetal.blockentity.MetalCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Generic implementation shared by both MetalCase and NetheriteCase blocks. TODO rename to clay case?
 */
public class MetalCaseBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // 0 = empty, 1 = filled
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 1);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final boolean returnsCase; // netherite variant returns empty case

    public MetalCaseBlock(Properties props) {
        this(props, false);
    }

    protected MetalCaseBlock(Properties props, boolean returnsCase) {
        super(props);
        this.returnsCase = returnsCase;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0).setValue(WATERLOGGED, false));
    }

    /**
     * Click with a molten metal bucket to fill (one action), or with a water bucket when full to cool.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        MetalCaseBlockEntity be = (MetalCaseBlockEntity) level.getBlockEntity(pos);
        if (be == null) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (be.handlePlayerInteraction(player, hand, held, returnsCase)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // ------------- Placement & Shape ----------------
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILL, WATERLOGGED);
    }

    // EntityBlock impl
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetalCaseBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.METAL_CASE.get() && !level.isClientSide ?
                (level1, pos, state1, blockEntity) -> MetalCaseBlockEntity.tick(level1, pos, state1, (MetalCaseBlockEntity) blockEntity) :
                null;
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.smeltingmetal.cool_with_water"));
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        // If no drops were added by the super method, add the block itself
        if (drops.isEmpty()) {
            drops.add(new ItemStack(this));
        }
        return drops;
    }
}
