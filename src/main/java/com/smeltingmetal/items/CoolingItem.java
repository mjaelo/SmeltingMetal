package com.smeltingmetal.items;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Base class for items that can cool down into other items when in water or after some time.
 */
public abstract class CoolingItem extends Item {
    protected static final int COOL_TICKS = 300; // 15 seconds

    protected CoolingItem(Properties properties) {
        super(properties);
    }

    /**
     * Called when the item should cool down.
     * @param stack The current item stack
     * @param level The level
     * @param pos The position of the item
     * @return The replacement item stack, or null if no replacement should happen
     */
    @Nullable
    protected abstract ItemStack getCooledItem(ItemStack stack, Level level, BlockPos pos);
    /**
     * Called after the item has been replaced by the cooled item.
     * @param originalStack The original item stack
     * @param cooledStack The new cooled item stack
     * @param level The level
     * @param pos The position of the item
     */
    protected void onCooled(ItemStack originalStack, ItemStack cooledStack, Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) return false;

        BlockPos pos = entity.blockPosition();
        boolean inWater = level.getFluidState(pos).is(FluidTags.WATER);
        boolean onFire = level.getBlockState(pos).is(Blocks.FIRE);
        boolean shouldCool = !onFire && (entity.getAge() >= COOL_TICKS || inWater);

        if (shouldCool) {
            ItemStack cooledStack = getCooledItem(stack, level, pos);
            if (cooledStack != null && !cooledStack.isEmpty()) {
                entity.setItem(cooledStack);
                onCooled(stack, cooledStack, level, pos);
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to get an item from a resource location.
     */
    protected static Item getItem(ResourceLocation id) {
        return ForgeRegistries.ITEMS.getValue(id);
    }
}
