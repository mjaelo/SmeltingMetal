package com.smeltingmetal.items.metalIngot;

import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.CoolingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Abstract base class for filled mold items that can cool into metal ingots.
 * This class provides common functionality for both hardened and netherite mold variants.
 * 
 * <p>Filled molds store the metal type they contain in their NBT data and can be cooled
 * to produce the corresponding metal ingot when exposed to water.</p>
 */
public abstract class AbstractFilledMoldItem extends CoolingItem {

    public static final String METAL_TYPE_NBT = "MetalType";

    protected AbstractFilledMoldItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /* -------------------------- NBT helpers --------------------------- */

    public static void setMetalType(ItemStack stack, String metalType) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(METAL_TYPE_NBT, metalType);
    }

    public static String getMetalType(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(METAL_TYPE_NBT)) {
            return stack.getTag().getString(METAL_TYPE_NBT);
        }
        return "unknown";
    }

    @Override
    protected ItemStack getCooledItem(ItemStack stack, Level level, BlockPos pos) {
        String metal = getMetalType(stack);
        if ("unknown".equals(metal)) return null;

        ResourceLocation ingotId = ModMetals.getMetalProperties(metal)
                .map(MetalProperties::ingotId)
                .orElse(null);
                
        if (ingotId == null) return null;
        
        Item ingotItem = CoolingItem.getItem(ingotId);
        if (ingotItem == null || ingotItem == Items.AIR) return null;

        // Spawn empty mold back if required
        if (keepMold()) {
            ItemStack moldStack = createEmptyMoldStack(stack.getCount());
            if (!moldStack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), moldStack));
            }
        }

        return new ItemStack(ingotItem, stack.getCount());
    }

    /**
     * @return true if the mold itself should be returned to the player/world after cooling.
     */
    protected abstract boolean keepMold();

    /**
     * Provide the empty mold stack that should be returned when {@link #keepMold()} is true.
     */
    protected ItemStack createEmptyMoldStack(int count) { 
        return ItemStack.EMPTY; 
    }


}
