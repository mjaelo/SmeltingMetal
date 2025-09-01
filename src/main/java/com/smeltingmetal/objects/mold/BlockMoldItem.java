package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        // Let the block handle the NBT data in setPlacedBy
        return true;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public String getShape() {
        return shape;
    }
}
