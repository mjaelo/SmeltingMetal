package com.smeltingmetal.items.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.EntityEventsUtils;
import com.smeltingmetal.items.molten.MoltenMetalBucket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockMold extends Block implements EntityBlock {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private final MaterialType materialType;
    private final String resultType = "block";

    public BlockMold(Properties properties, MaterialType materialType) {
        super(properties);
        this.materialType = materialType;
        this.registerDefaultState(this.stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockMoldEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (materialType == MaterialType.CLAY) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BlockMoldEntity be)) {
            return InteractionResult.FAIL;
        }

        boolean isFilled = be.isFilled();
        if (state.getValue(FILLED) != isFilled) {
            level.setBlock(pos, state.setValue(FILLED, isFilled), 3);
        }

        ItemStack heldStack = player.getItemInHand(hand);
        if (onRightClickBlockMold(pos, player, be, heldStack)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public boolean onRightClickBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack) {
        Level level = player.level();
        if (heldStack.getItem() instanceof MoltenMetalBucket) {
            MetalProperties metalProps = ModMetals.getMetalPropertiesFromStack(heldStack);
            if (metalProps == null) return false;
            return EntityEventsUtils.fillBlockMold(level,pos,player, be, heldStack, metalProps.name());
        } else if (heldStack.getItem() instanceof BucketItem bucketItem) {
            if (bucketItem.getFluid() == Fluids.EMPTY)
                return EntityEventsUtils.fillBucketFromBlockMold(pos, player, be, heldStack, level);
            else if (bucketItem.getFluid() == Fluids.WATER)
                return EntityEventsUtils.coolBlockMold(pos, player, be, heldStack, level);
            else {
                for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
                    Item bucket = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
                    if (bucket == bucketItem) return EntityEventsUtils.fillBlockMold(level,pos,player, be, heldStack, metalProps.name());
                }
                return false;
            }
        }
        return false;
    }
}
