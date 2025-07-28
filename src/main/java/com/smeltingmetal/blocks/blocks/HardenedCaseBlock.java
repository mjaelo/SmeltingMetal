package com.smeltingmetal.blocks.blocks;

import com.smeltingmetal.blocks.blockentity.MetalCaseBlockEntity;
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
 * Represents a hardened case block used in the metal smelting process.
 * This is the base class for both regular and netherite variants of the metal case.
 * The case can be filled with molten metal and will convert to a solid metal block when cooled with water.
 */
public class HardenedCaseBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // 0 = empty, 1 = filled
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 1);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public HardenedCaseBlock(Properties props) {
        super(props);
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
        if (be.handlePlayerInteraction(player, held)) {
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
