package com.smeltingmetal.items;

import com.smeltingmetal.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilledMoldItem extends Item {
    public static final String METAL_TYPE_NBT = "MetalType";

    public FilledMoldItem(Properties pProperties) {
        super(pProperties);
    }

    // --- NBT Helper Methods ---
    public static ItemStack createFilledMold(String metalType) {
        ItemStack stack = new ItemStack(ModItems.FILLED_MOLD.get());
        setMetalType(stack, metalType);
        return stack;
    }

    public static void setMetalType(ItemStack stack, String metalType) {
        CompoundTag nbt = stack.getOrCreateTag();
        nbt.putString(METAL_TYPE_NBT, metalType);
    }

    public static String getMetalType(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(METAL_TYPE_NBT)) {
            return stack.getTag().getString(METAL_TYPE_NBT);
        }
        return "unknown"; // Default or error case
    }

    // --- Display Name & Tooltip ---
    @Override
    public Component getName(ItemStack stack) {
        String metalType = getMetalType(stack);
        // Lang key example: item.smeltingmetal.filled_mold_of
        return Component.translatable("item.smeltingmetal.filled_mold_of", Component.translatable("metal.smeltingmetal." + metalType));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        String metalType = getMetalType(pStack);
        if (!"unknown".equals(metalType)) {
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.metalType", Component.translatable("metal.smeltingmetal." + metalType)).withStyle(ChatFormatting.GRAY));
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.cool_in_water").withStyle(ChatFormatting.BLUE));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.unknown_metal").withStyle(ChatFormatting.RED));
        }
    }
}