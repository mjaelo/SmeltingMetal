package com.smeltingmetal.items;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.generic.MetalItem;
import com.smeltingmetal.items.mold.BlockMoldEntity;
import com.smeltingmetal.items.mold.ItemMold;
import com.smeltingmetal.items.molten.MoltenMetalBlockItem;
import com.smeltingmetal.items.molten.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
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

    public static boolean handleStackAutoCooling(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;
        BlockPos pos = entity.blockPosition();
        boolean inWater = level.getFluidState(pos).is(FluidTags.WATER);
        boolean onFire = level.getBlockState(pos).is(Blocks.FIRE);
        boolean shouldCool = !onFire && (entity.getAge() >= COOL_TICKS || inWater);
        if (!shouldCool) return false;

        ItemStack cooledStack = stack.getItem() instanceof ItemMold ? getMetalItemFromItemMoldStack(stack) : getRawItemFromMoltenStack(stack);
        if (cooledStack == null || cooledStack.isEmpty()) return false;
        entity.setItem(cooledStack);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        return true;

    }

    public static boolean fillBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack, Level level) {
        if (be.isFilled()) {
            return false;
        }

        MetalProperties metalProps = ModMetals.getMetalPropertiesFromStack(heldStack);
        if (metalProps == null) {
            return false;
        }

        // Fill the mold with metal
        be.fill(metalProps.name());

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
        // Handle emptying the mold with an empty bucket
        if (!be.isFilled()) {
            return false;
        }

        // Get the metal type and create a molten metal bucket
        String metalType = be.getMetalType();
        MetalProperties metalProps = ModMetals.getMetalProperties(metalType);
        if (metalProps == null) {
            return false;
        }

        // Create molten metal bucket
        ItemStack moltenBucket = new ItemStack(ModItems.MOLTEN_METAL_BUCKET.get());
        ModMetals.setMetalTypeToStack(moltenBucket, metalType);

        // Replace the empty bucket with a molten metal bucket
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            if (heldStack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, moltenBucket);
            } else if (!player.getInventory().add(moltenBucket)) {
                player.drop(moltenBucket, false);
            }
        }

        // Empty the mold
        be.empty();
        level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static ItemStack getRawItemFromMoltenStack(ItemStack stack) {
        MetalProperties metalProps = null;
        Item cooledItem = null;
        if (stack.getItem() instanceof MoltenMetalItem) {
            metalProps = ModMetals.getMetalPropertiesFromStack(stack);
            cooledItem = ForgeRegistries.ITEMS.getValue(metalProps.raw());
        } else if (stack.getItem() instanceof MoltenMetalBlockItem) {
            metalProps = ModMetals.getMetalPropertiesFromStack(stack);
            cooledItem = ForgeRegistries.ITEMS.getValue(metalProps.rawBlock());
        }

        if (metalProps == null || cooledItem == null || cooledItem == Items.AIR) return null;
        ItemStack result = new ItemStack(cooledItem);

        if (cooledItem instanceof MetalItem) {
            ModMetals.setMetalTypeToStack(result, metalProps.name());
        }

        return result;
    }

    private static ItemStack getMetalItemFromItemMoldStack(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemMold)) return null;
        MetalProperties metalProps = ModMetals.getMetalPropertiesFromStack(stack);
        if (metalProps == null) return stack;
        ResourceLocation rawId = metalProps.ingot();
        Item cooledItem = ForgeRegistries.ITEMS.getValue(rawId);
        if (cooledItem == null) return stack;
        ItemStack result = new ItemStack(cooledItem);
        if (cooledItem == Items.AIR) return stack;
        if (cooledItem instanceof MetalItem) {
            ModMetals.setMetalTypeToStack(result, ModMetals.getMetalTypeFromStack(stack));
        }
        return result;
    }

}
