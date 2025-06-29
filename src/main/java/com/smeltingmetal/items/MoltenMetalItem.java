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

public class MoltenMetalItem extends Item {
    public static final String METAL_TYPE_NBT = "MetalType";

    public MoltenMetalItem(Properties pProperties) {
        super(pProperties);
    }

    public static ItemStack createMoltenMetal(String metalType) {
        ItemStack stack = new ItemStack(ModItems.MOLTEN_METAL.get());
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
        return "unknown";
    }

    @Override
    public Component getName(ItemStack stack) {
        String metalType = getMetalType(stack);
        return Component.translatable("item.smeltingmetal.molten_metal_of", Component.translatable("metal.smeltingmetal." + metalType));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        String metalType = getMetalType(pStack);
        if (!"unknown".equals(metalType)) {
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.metalType", Component.translatable("metal.smeltingmetal." + metalType)).withStyle(ChatFormatting.GRAY));
            // NEW: Add instruction for making filled mold
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.make_filled_mold").withStyle(ChatFormatting.BLUE));
        } else {
            pTooltipComponents.add(Component.translatable("tooltip.smeltingmetal.unknown_molten_metal").withStyle(ChatFormatting.RED));
        }
    }
}