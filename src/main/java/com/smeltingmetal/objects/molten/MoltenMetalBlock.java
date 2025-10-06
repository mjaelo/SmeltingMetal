package com.smeltingmetal.objects.molten;

import com.smeltingmetal.objects.generic.MetalItem;
import com.smeltingmetal.utils.EntityEventsUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class MoltenMetalBlock extends MetalItem {
    public MoltenMetalBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }
}
