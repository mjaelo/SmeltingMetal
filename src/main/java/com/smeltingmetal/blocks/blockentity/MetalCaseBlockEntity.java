package com.smeltingmetal.blocks.blockentity;

import com.smeltingmetal.blocks.ModBlockEntities;
import com.smeltingmetal.blocks.blocks.HardenedCaseBlock;
import com.smeltingmetal.items.metalBlock.MoltenMetalBucketItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Stores up to one MoltenMetalItem of the same metal-id.  On water contact when full
 * it converts to the corresponding metal-block.  Netherite variant will return the empty
 * case item to the world (handled by caller via flag).
 */
public class MetalCaseBlockEntity extends BlockEntity {

    private String metalType; // null until first insertion

    public MetalCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.hardened_case.get(), pos, state);
    }


    /**
     * Player right-click interaction from the block class.
     * Handles filling from molds and immediate water cooling.
     */
    public boolean handlePlayerInteraction(Player player, ItemStack held) {
        Level lvl = getLevel();
        if (lvl == null) return false;

        // Water bucket cooling
        if (metalType != null && held.getItem() == Items.WATER_BUCKET) {
            // consume water bucket (unless creative)
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(empty)) player.drop(empty, false);
            }

            performCooling();
            return true;
        }

        // Molten bucket pouring
        if (metalType == null && held.getItem() instanceof MoltenMetalBucketItem) {
            String bucketMetalId = MoltenMetalBucketItem.getMetalId(held);
            if (bucketMetalId == null) return false;
            metalType = bucketMetalId;
            if (!player.getAbilities().instabuild) {
                // consume bucket and give empty one
                held.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(empty)) player.drop(empty, false);
            }

            // Update block state to filled
            if (level != null) {
                BlockState newState = getBlockState().setValue(HardenedCaseBlock.FILL, 1);
                level.setBlock(worldPosition, newState, 3);
                setChanged();
            }
            // sound
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    private void performCooling() {
        Level level1 = getLevel();
        if (level1 == null) return;

        // determine block to set (<metal>_block) fallback iron_block
        String[] parts = metalType.split(":", 2);
        String ns = parts.length == 2 ? parts[0] : "minecraft";
        String path = parts.length == 2 ? parts[1] : parts[0];
        ResourceLocation outId = new ResourceLocation(ns, path + "_block");
        Block outBlock = ForgeRegistries.BLOCKS.getValue(outId);
        if (outBlock == null || outBlock == Blocks.AIR) outBlock = Blocks.IRON_BLOCK;

        // Replace block
        level1.setBlock(worldPosition, outBlock.defaultBlockState(), 3);

        // Drop empty case if needed
//        if (getBlockState().getBlock() == ModBlocks.NETHERITE_CASE.get()) {
//            ItemStack emptyCase = new ItemStack(ModItems.NETHERITE_CASE.get());
//            ItemEntity e = new ItemEntity(level1, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, emptyCase);
//            level1.addFreshEntity(e);
//        }

        level1.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
        if (level1 instanceof ServerLevel slvl) {
            slvl.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.01);
        }
    }

}
