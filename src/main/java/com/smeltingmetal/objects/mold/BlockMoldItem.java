package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import net.minecraft.world.level.block.Block;

public class BlockMoldItem extends MetalBlockItem {
    private final MaterialType materialType;
    private String shape;

    public BlockMoldItem(Block pBlock, Properties pProperties, MaterialType materialType) {
        super(pBlock, pProperties);
        this.materialType = materialType;
    }
    public BlockMoldItem(Block pBlock, Properties pProperties, MaterialType materialType, String shape) {
        super(pBlock, pProperties);
        this.materialType = materialType;
        this.shape = shape;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public String getShape() {
        return shape;
    }
}
