package com.smeltingmetal.items.molten;

import com.smeltingmetal.items.generic.MetalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class MoltenMetalBlock extends MetalBlock {
    public MoltenMetalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

}
