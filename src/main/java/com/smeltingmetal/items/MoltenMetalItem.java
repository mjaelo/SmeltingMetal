package com.smeltingmetal.items;

import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class MoltenMetalItem extends Item {
    public static final String METAL_ID_KEY = "MetalID";

    public MoltenMetalItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        String metalId = getMetalId(pStack);
        if (metalId != null) {
            String idPath = metalId.contains(":") ? metalId.split(":")[1] : metalId;
            String formattedName = idPath.substring(0,1).toUpperCase() + idPath.substring(1);
            return Component.literal("Molten " + formattedName);
        }
        return super.getName(pStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltip, pIsAdvanced);
        String metalId = getMetalId(pStack);
        if (metalId != null) {
            String idPath = metalId.contains(":") ? metalId.split(":")[1] : metalId;
            pTooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".metalType", idPath));
        }
    }

    // fillItemCategory has been removed as it is handled by the BuildCreativeModeTabContentsEvent in ModItems

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
                    .map(props -> props.ingotId())
                    .orElse(null);
        }
        return null;
    }
}