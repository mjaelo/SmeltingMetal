package com.smeltingmetal.objects.generic;

import com.smeltingmetal.objects.mold.BlockMoldItem;
import com.smeltingmetal.utils.MetalUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MetalBlockItem extends BlockItem {
    public MetalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component originalName = super.getName(stack);
        String newName = originalName.getString();
        if (this instanceof BlockMoldItem itemMold && itemMold.getShape() != null) {
            newName = Component.translatable("block.smeltingmetal.block_mold_clay").getString();
            MetalUtils.setShapeToStack(stack, itemMold.getShape(), true);
        }
        String metalName = MetalUtils.capitalizeString(MetalUtils.getMetalTypeFromStack(stack));
        String resultType = MetalUtils.capitalizeString(MetalUtils.getShapeFromStack(stack));
        return Component.literal(newName.replace(
                "Block", metalName + " " + resultType
        ));
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
}
