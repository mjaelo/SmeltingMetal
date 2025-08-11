package com.smeltingmetal.blocks.blockentity;

import com.smeltingmetal.blocks.ModBlockEntities;
import com.smeltingmetal.blocks.blocks.HardenedCaseBlock;
import com.smeltingmetal.data.ModMetals;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

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
    
    private static final String METAL_TYPE_KEY = "MetalType";
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(METAL_TYPE_KEY)) {
            this.metalType = tag.getString(METAL_TYPE_KEY);
        }
        // Ensure block state matches the metalType when loaded
        if (level != null && !level.isClientSide && metalType == null) {
            BlockState currentState = getBlockState();
            if (currentState.getValue(HardenedCaseBlock.FILL) != 0) {
                level.setBlock(worldPosition, currentState.setValue(HardenedCaseBlock.FILL, 0), 3);
            }
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.metalType != null) {
            tag.putString(METAL_TYPE_KEY, this.metalType);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && metalType == null) {
            // If metalType is null, ensure block state is empty
            BlockState currentState = getBlockState();
            if (currentState.getValue(HardenedCaseBlock.FILL) != 0) {
                level.setBlock(worldPosition, currentState.setValue(HardenedCaseBlock.FILL, 0), 3);
            }
        }
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

        // Empty bucket filling
        else if (metalType != null && held.getItem() == Items.BUCKET) {
            // Create the appropriate bucket item based on metal properties
            ItemStack filledBucket = ModMetals.getMetalProperties(metalType)
                    .map(metalProps -> metalProps.bucketId() != null
                            ? new ItemStack(ForgeRegistries.ITEMS.getValue(metalProps.bucketId()))
                            : MoltenMetalBucketItem.createStack(metalType))
                    .orElse(ItemStack.EMPTY);

            if (filledBucket.isEmpty()) return false;

            if (!player.getAbilities().instabuild) {
                // Consume the empty bucket
                held.shrink(1);
                
                // Give the filled bucket to the player
                if (!player.getInventory().add(filledBucket)) {
                    player.drop(filledBucket, false);
                }
            }

            // Update block state to empty
            if (level != null) {
                BlockState newState = getBlockState().setValue(HardenedCaseBlock.FILL, 0);
                level.setBlock(worldPosition, newState, 3);
                metalType = null; // Clear the metal type
                setChanged();
            }
            
            // Play sound
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }

        // Molten bucket pouring
        else if (metalType == null) {
            // Check if held item matches any bucketId in METAL_PROPERTIES_MAP
            String declaredBucketId = ModMetals.getAllMetalProperties().entrySet().stream()
                    .filter(entry -> entry.getValue().bucketId() != null &&
                            ForgeRegistries.ITEMS.getKey(held.getItem()).equals(entry.getValue().bucketId()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            // set generic metal type if no declared bucketId found
            metalType = declaredBucketId == null ? MoltenMetalBucketItem.getMetalId(held) : declaredBucketId;

            if (metalType == null) return false;

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
