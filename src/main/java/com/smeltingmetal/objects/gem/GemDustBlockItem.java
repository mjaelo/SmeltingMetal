package com.smeltingmetal.objects.gem;

import com.smeltingmetal.objects.generic.MetalBlockItem;
import com.smeltingmetal.utils.EntityEventsUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GemDustBlockItem extends MetalBlockItem {
    public GemDustBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }
}
