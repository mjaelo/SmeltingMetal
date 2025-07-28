package com.smeltingmetal.items.metalIngot;

import com.smeltingmetal.items.ModItems;
import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Similar to {@link FilledMoldItem} but the mold does not break after cooling.
 */
public class FilledNetheriteMoldItem extends AbstractFilledMoldItem {
    public FilledNetheriteMoldItem(Properties pProperties) {
        super(pProperties);
    }

    public static ItemStack createFilled(String metalType) {
        ItemStack stack = new ItemStack(ModItems.FILLED_NETHERITE_MOLD.get());
        setMetalType(stack, metalType);
        return stack;
    }

    public static void setMetalType(ItemStack stack, String metalType) {
        AbstractFilledMoldItem.setMetalType(stack, metalType);
    }

    public static String getMetalType(ItemStack stack) {
        return AbstractFilledMoldItem.getMetalType(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String metal = getMetalType(stack);
        String idPath = metal.contains(":") ? metal.split(":")[1] : metal;
        String formatted = idPath.substring(0,1).toUpperCase() + idPath.substring(1);
        return Component.literal("Filled Netherite " + formatted + " Mold");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String metal = getMetalType(stack);
        if (!"unknown".equals(metal)) {
            tooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".cool_in_water"));
        } else {
            tooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".unknown_metal"));
        }
    }

    @Override
    protected boolean keepMold() {
        return true;
    }

    @Override
    protected ItemStack createEmptyMoldStack(int count) {
        return new ItemStack(ModItems.NETHERITE_MOLD.get(), count);
    }
}
