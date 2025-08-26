package com.smeltingmetal.items.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.items.generic.MetalBlockItem;
import net.minecraft.world.level.block.Block;

public class BlockMoldItem extends MetalBlockItem {
    private MaterialType materialType;
    private String resultType = "block";

    public BlockMoldItem(Block pBlock, Properties pProperties, MaterialType materialType) {
        super(pBlock, pProperties);
        this.materialType = materialType;
    }
}
