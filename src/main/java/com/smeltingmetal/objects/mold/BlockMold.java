package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.objects.molten.MoltenMetalBucket;
import com.smeltingmetal.utils.EntityEventsUtils;
import com.smeltingmetal.utils.MetalUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class BlockMold extends Block implements EntityBlock {
    public static final BooleanProperty HAS_METAL = BooleanProperty.create(ModMetals.METAL_KEY);
    public static final IntegerProperty SHAPE = IntegerProperty.create(ModMetals.SHAPE_KEY, 0, 4);
    private final MaterialType materialType;

    public BlockMold(Properties properties, MaterialType materialType) {
        super(properties);
        this.materialType = materialType;
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_METAL, false).setValue(SHAPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_METAL, SHAPE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockMoldEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Get the block entity and set its shape from the item's NBT
        if (level.getBlockEntity(pos) instanceof BlockMoldEntity blockEntity) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(ModMetals.SHAPE_KEY)) {
                String shape = tag.getString(ModMetals.SHAPE_KEY);
                blockEntity.setShapeType(shape);

                // Update block state to match the shape
                int shapeIndex = blockEntity.getShapeIndex();
                level.setBlock(pos, state.setValue(SHAPE, shapeIndex), 3);
            }

            // Also handle metal type if present
            if (tag != null && tag.contains(ModMetals.METAL_KEY)) {
                String metalType = tag.getString(ModMetals.METAL_KEY);
                if (!metalType.isEmpty()) {
                    blockEntity.fill(metalType);
                    level.setBlock(pos, state.setValue(HAS_METAL, true), 3);
                }
            }
        }
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

        ItemStack heldStack = player.getItemInHand(hand);
        if (onRightClickBlockMold(pos, player, be, heldStack)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack stack = new ItemStack(this);

        BlockPos pos = BlockPos.containing(builder.getParameter(LootContextParams.ORIGIN));
        BlockEntity blockEntity = builder.getLevel().getBlockEntity(pos);

        if (blockEntity instanceof BlockMoldEntity be) {
            // Only set shape if it's not empty
            if (!be.getShapeType().isEmpty()) {
                MetalUtils.setShapeToStack(stack, be.getShapeType(), true);
            }
            // Only set metal if it's not empty
            if (!be.getMetalType().isEmpty()) {
                MetalUtils.setMetalToStack(stack, be.getMetalType());
            }
        }

        return List.of(stack);
    }

    public boolean onRightClickBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack) {
        Level level = player.level();
        boolean result = false;

        if (heldStack.getItem() instanceof MoltenMetalBucket) {
            MetalProperties metalProps = MetalUtils.getMetalPropertiesFromStack(heldStack);
            if (metalProps != null) {
                result = EntityEventsUtils.fillBlockMold(level, pos, player, be, heldStack, metalProps.name());
            }
        } else if (heldStack.getItem() instanceof BucketItem bucketItem) {
            if (bucketItem.getFluid() == Fluids.EMPTY) {
                result = EntityEventsUtils.fillBucketFromBlockMold(pos, player, be, heldStack, level);
            } else if (bucketItem.getFluid() == Fluids.WATER) {
                result = EntityEventsUtils.coolBlockMold(pos, player, be, heldStack, level);
            } else {
                for (MetalProperties metalProps : MetalUtils.getAllMetalProperties().values()) {
                    Item bucket = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
                    if (bucket == bucketItem) {
                        result = EntityEventsUtils.fillBlockMold(level, pos, player, be, heldStack, metalProps.name());
                        break;
                    }
                }
            }
        }

        // Update block state after any interaction
        if (result && !level.isClientSide) {
            BlockState currentState = level.getBlockState(pos);
            boolean hasMetal = be.hasMetal();
            int shape = be.getShapeIndex();

            if (currentState.getValue(HAS_METAL) != hasMetal || currentState.getValue(SHAPE) != shape) {
                level.setBlock(pos, currentState.setValue(HAS_METAL, hasMetal).setValue(SHAPE, shape), 3);
            }
        }

        return result;
    }
}
