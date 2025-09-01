package com.smeltingmetal.objects.molten;

import com.smeltingmetal.utils.EntityEventsUtils;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class MoltenMetalBlockItem extends MetalBlockItem {
    public MoltenMetalBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }
}
