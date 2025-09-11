package com.smeltingmetal.objects.generic;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.objects.gem.GemDustItem;
import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.utils.ModUtils;
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
    public Component getName(ItemStack stack) {
        Component originalName = super.getName(stack);
        String newName = originalName.getString();
        if (this instanceof ItemMold itemMold && itemMold.getMaterialType() == MaterialType.CLAY) {
            newName = Component.translatable("item.smeltingmetal.item_mold_clay").getString();
        }
        String contentName = ModUtils.capitalizeString(ModUtils.getContentFromStack(stack));
        String shapeName = ModUtils.capitalizeString(ModUtils.getShapeFromStack(stack));
        boolean isDefaultContent = contentName.equals(ModUtils.capitalizeString(ModData.DEFAULT_CONTENT));
        boolean isDefaultShape = shapeName.equals(ModUtils.capitalizeString(ModData.DEFAULT_ITEM_SHAPE));

        String replacement = this instanceof ItemMold
                ? (isDefaultContent ? "" : contentName + " ") + (isDefaultShape ? "": shapeName + " ")
                : this instanceof GemDustItem && isDefaultContent
                    ? ModUtils.capitalizeString(ModData.DEFAULT_GEM)
                    : contentName;

        return Component.literal(newName.replace("%s", replacement));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String baseKey = this.getDescriptionId() + ".tooltip";
        if (this instanceof ItemMold itemMold && itemMold.getMaterialType() == MaterialType.CLAY) {
            baseKey = "item.smeltingmetal.item_mold_clay.tooltip";
            ModUtils.setShapeToStack(stack, itemMold.getShape(), false);
        }
        if (Component.translatable(baseKey).getString().equals(baseKey)) {
            return; // No tooltip found
        }
        String tooltipText = Component.translatable(baseKey).getString();
        tooltip.add(Component.literal(tooltipText));
    }

}
