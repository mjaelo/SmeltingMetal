package com.smeltingmetal.items.metalIngot;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.CoolingItem;
import com.smeltingmetal.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents an item containing molten metal that can cool into a solid form.
 * This class handles the behavior of molten metal items, including their display name,
 * tooltip information, and cooling mechanics.
 *
 * <p>Molten metal items store their metal type in NBT data and can cool into
 * the corresponding metal ingot when exposed to water.</p>
 */
public class MoltenMetalItem extends CoolingItem {
    public static final String METAL_ID_KEY = "MetalID";

    public MoltenMetalItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        String metalId = getMetalId(pStack);
        if (metalId != null) {
            String idPath = metalId.contains(":") ? metalId.split(":")[1] : metalId;
            String formattedName = idPath.substring(0, 1).toUpperCase() + idPath.substring(1);
            return Component.literal("Molten " + formattedName);
        }
        return super.getName(pStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltip, pIsAdvanced);
        String metalId = getMetalId(pStack);
        if (metalId != null) {
            pTooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".make_filled_mold"));
        }
    }

    public static ItemStack createStack(String metalId) {
        ItemStack stack = new ItemStack(ModItems.MOLTEN_METAL.get());
        stack.getOrCreateTag().putString(METAL_ID_KEY, metalId);
        return stack;
    }

    public static String getMetalId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(METAL_ID_KEY)) {
            return tag.getString(METAL_ID_KEY);
        }
        return null;
    }

    public ResourceLocation getIngotId(ItemStack stack) {
        String metalId = getMetalId(stack);
        if (metalId != null) {
            return ModMetals.getMetalProperties(metalId)
                    .map(MetalProperties::ingotId)
                    .orElse(null);
        }
        return null;
    }

    @Override
    protected ItemStack getCooledItem(ItemStack stack, Level level, BlockPos pos) {
        String metalId = getMetalId(stack);
        if (metalId == null || !ModMetals.doesMetalExist(metalId)) {
            return null;
        }

        // Get the metal properties to access the raw item ID
        return ModMetals.getMetalProperties(metalId)
                .map(metalProps -> {
                    // Use the raw item ID from MetalProperties
                    Item rawItem = ForgeRegistries.ITEMS.getValue(metalProps.rawId());
                    if (rawItem != null && rawItem != Items.AIR) {
                        return new ItemStack(rawItem, stack.getCount());
                    }
                    // Fallback to ingot if raw item is not available (shouldn't happen as raw is required)
                    rawItem = ForgeRegistries.ITEMS.getValue(metalProps.ingotId());
                    return rawItem != null && rawItem != Items.AIR 
                            ? new ItemStack(rawItem, stack.getCount()) 
                            : null;
                })
                .orElse(null);
    }

}