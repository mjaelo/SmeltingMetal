package com.smeltingmetal.items;

import com.smeltingmetal.ModItems;
import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilledMoldItem extends AbstractFilledMoldItem {
    public FilledMoldItem(Properties pProperties) {
        super(pProperties);
    }

    public static ItemStack createFilledMold(String metalType) {
        ItemStack stack = new ItemStack(ModItems.FILLED_MOLD.get());
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
        String metalType = getMetalType(stack);
        String idPath = metalType.contains(":") ? metalType.split(":")[1] : metalType;
        String formatted = idPath.substring(0,1).toUpperCase() + idPath.substring(1);
        return Component.literal("Filled " + formatted + " Mold");
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        String metalType = getMetalType(pStack);
        if (!"unknown".equals(metalType)) {
            String idPath = metalType.contains(":") ? metalType.split(":")[1] : metalType;
            pTooltipComponents.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".cool_in_water"));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".unknown_metal"));
        }
    }

    @Override
    protected boolean keepMold() {
        return false; // Hardened mold breaks after use
    }
}