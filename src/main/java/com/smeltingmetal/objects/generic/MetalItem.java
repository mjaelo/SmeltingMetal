package com.smeltingmetal.objects.generic;

import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.utils.MetalUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MetalItem extends Item {

    public MetalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String baseKey = this.getDescriptionId() + ".tooltip";
        if (Component.translatable(baseKey).getString().equals(baseKey)) {
            return; // No tooltip found
        }
        String tooltipText = Component.translatable(baseKey).getString();
        tooltip.add(Component.literal(tooltipText));
    }

    @Override
    public Component getName(ItemStack stack) {
        Component originalName = super.getName(stack);
        String newName = originalName.getString();
        if (this instanceof ItemMold itemMold && itemMold.getShape() != null) {
            newName = Component.translatable("item.smeltingmetal.item_mold_clay").getString();
        }
        String metalName = MetalUtils.capitalizeString(MetalUtils.getMetalTypeFromStack(stack));
        String resultType = MetalUtils.capitalizeString(MetalUtils.getShapeFromStack(stack));
        return Component.literal(String.format(newName, metalName, resultType));
    }
}
