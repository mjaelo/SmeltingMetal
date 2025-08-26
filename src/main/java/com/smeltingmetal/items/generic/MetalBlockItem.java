package com.smeltingmetal.items.generic;

import com.smeltingmetal.init.ModMetals;
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
        String metalType = ModMetals.getMetalTypeFromStack(stack);
        String metalName = metalType.substring(0, 1).toUpperCase() + metalType.substring(1);
        return Component.literal(originalName.getString().replace("%s", metalName));
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
