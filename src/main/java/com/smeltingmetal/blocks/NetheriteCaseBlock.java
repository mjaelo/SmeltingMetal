package com.smeltingmetal.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Netherite variant of the metal case block that returns the empty case when broken.
 */
public class NetheriteCaseBlock extends HardenedCaseBlock {

    public NetheriteCaseBlock(Properties props) {
        super(props, true);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        // Call parent first to handle any parent logic
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        
        // Then add our drop if in survival mode
        if (!level.isClientSide && !player.isCreative()) {
            popResource(level, pos, new ItemStack(this));
        }
    }
    
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        // If no drops were added, add the block itself
        if (drops.isEmpty()) {
            drops.add(new ItemStack(this));
        }
        return drops;
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            // Drop the block itself when removed
            popResource(level, pos, new ItemStack(this));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
