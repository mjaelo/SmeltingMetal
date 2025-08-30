package com.smeltingmetal.items;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.mold.BlockMoldEntity;
import com.smeltingmetal.items.molten.MoltenMetalBucket;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityEventsUtils {

    protected static final int COOL_TICKS = 300; // 15 seconds

    public static boolean handleStackAutoCooling(ItemStack containerStack, ItemEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (level.getBlockState(pos).is(Blocks.FIRE)) return false;
        boolean shouldCool = entity.getAge() >= COOL_TICKS;

        ItemStack cooledStack = ServerEventsUtils.getCooledMetalStack(containerStack, level, pos, shouldCool);
        if (cooledStack == null || cooledStack.isEmpty()) return false;

        // Drop the cooled item
        if (!containerStack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    containerStack);
            level.addFreshEntity(itemEntity);
        }
        entity.setItem(cooledStack);
        return true;
    }

    public static boolean fillBlockMold(Level level, BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack, String metalName) {
        if (be.isFilled()) {
            return false;
        }
        be.fill(metalName);

        // Replace the molten metal bucket with an empty bucket
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
        }

        // Play sound and return success
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static boolean coolBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack, Level level) {
        if (!be.isFilled()) return false;
        String metalType = be.getMetalType();
        MetalProperties metalProps = ModMetals.getMetalProperties(metalType);
        if (metalProps == null) return false;

        // Create the resulting metal block
        Item blockId = ForgeRegistries.ITEMS.getValue(metalProps.block());
        if (blockId == null) return false;
        ItemStack resultStack = new ItemStack(blockId);
        ModMetals.setMetalTypeToStack(resultStack, metalType);

        // Drop the resulting item
        ItemEntity itemEntity = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                resultStack);
        level.addFreshEntity(itemEntity);

        // Handle mold based on material type
        MaterialType moldMaterial = be.getMaterialType();
        if (moldMaterial == MaterialType.NETHERITE) {
            be.empty();
        } else {
            level.destroyBlock(pos, false);
        }

        // Replace water bucket with empty bucket
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            ItemStack emptyBucket = new ItemStack(Items.BUCKET);
            if (heldStack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, emptyBucket);
            } else if (!player.getInventory().add(emptyBucket)) {
                player.drop(emptyBucket, false);
            }
        }

        // Play sound and return success
        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static boolean fillBucketFromBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack, Level level) {
        if (!be.isFilled()) return false;
        String metalType = be.getMetalType();
        MetalProperties metalProps = ModMetals.getMetalProperties(metalType);
        if (metalProps == null) return false;

        Item bucketItem = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
        if (bucketItem == null) return false;
        ItemStack bucketStack = new ItemStack(bucketItem);
        if (bucketItem instanceof MoltenMetalBucket) {
            ModMetals.setMetalTypeToStack(bucketStack, metalType);
        }

        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            if (heldStack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, bucketStack);
            } else if (!player.getInventory().add(bucketStack)) {
                player.drop(bucketStack, false);
            }
        }

        // Empty the mold
        be.empty();
        level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

}
