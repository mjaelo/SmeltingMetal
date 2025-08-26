package com.smeltingmetal.items.molten;

import com.smeltingmetal.items.EntityEventsUtils;
import com.smeltingmetal.items.generic.MetalItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class MoltenMetalItem extends MetalItem {
    public MoltenMetalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }
}
