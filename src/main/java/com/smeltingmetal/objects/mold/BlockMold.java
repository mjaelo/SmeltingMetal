package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.objects.gem.GemDustItem;
import com.smeltingmetal.objects.molten.MoltenMetalBucket;
import com.smeltingmetal.utils.EntityEventsUtils;
import com.smeltingmetal.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class BlockMold extends Block implements EntityBlock {
    public static final IntegerProperty CONTENT = IntegerProperty.create(ModData.CONTENT_KEY, 0, 2);
    public static final IntegerProperty SHAPE = IntegerProperty.create(ModData.SHAPE_KEY, 0, 4);
    private final MaterialType materialType;

    public BlockMold(Properties properties, MaterialType materialType) {
        super(properties);
        this.materialType = materialType;
        this.registerDefaultState(this.stateDefinition.any().setValue(CONTENT, 0).setValue(SHAPE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT, SHAPE);
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
            if (tag != null && tag.contains(ModData.SHAPE_KEY)) {
                String shape = tag.getString(ModData.SHAPE_KEY);
                blockEntity.setShape(shape);

                // Update block state to match the shape
                int shapeIndex = ModUtils.getItemShapeId(blockEntity.getShape());
                level.setBlock(pos, state.setValue(SHAPE, shapeIndex), 3);
            }

            // Also handle metal type if present
            if (tag != null && tag.contains(ModData.CONTENT_KEY)) {
                String metalType = tag.getString(ModData.CONTENT_KEY);
                if (!metalType.isEmpty()) {
                    blockEntity.setContent(metalType);
                    level.setBlock(pos, state.setValue(CONTENT, 1), 3);
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
            if (!be.getShape().isEmpty()) {
                ModUtils.setShapeToStack(stack, be.getShape(), true);
            }
            // Only set metal if it's not empty
            if (!be.getContent().isEmpty()) {
                ModUtils.setContentToStack(stack, be.getContent());
            }
        }

        return List.of(stack);
    }

    public boolean onRightClickBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack) {
        Level level = player.level();
        boolean result = false;
        SoundEvent sound = null;

        if (heldStack.getItem() instanceof MoltenMetalBucket) {
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(heldStack);
            if (metalProps != null) {
                result = EntityEventsUtils.fillBlockMold( player, be, heldStack, metalProps.name());
                sound = SoundEvents.BUCKET_EMPTY_LAVA;
            }
        } else if (heldStack.getItem() instanceof BucketItem bucketItem) {
            if (bucketItem.getFluid() == Fluids.EMPTY) {
                result = EntityEventsUtils.fillBucketFromBlockMold(player, be, heldStack);
                sound = SoundEvents.BUCKET_FILL_LAVA;
            } else if (be.getContentType() == 1 && bucketItem.getFluid() == Fluids.WATER) {
                result = EntityEventsUtils.coolBlockMold(pos, player, be, heldStack, level);
                sound = SoundEvents.GENERIC_EXTINGUISH_FIRE;
            } else if (be.getContentType() == 2 && bucketItem.getFluid() == Fluids.LAVA) {
                result = EntityEventsUtils.coolBlockMold(pos, player, be, heldStack, level);
                sound = SoundEvents.GENERIC_EXTINGUISH_FIRE;
            } else if (!be.hasContent()) {
                for (MetalProperties metalProps : ModUtils.getAllMetalProperties().values()) {
                    Item bucket = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
                    if (bucket == bucketItem) {
                        result = EntityEventsUtils.fillBlockMold(player, be, heldStack, metalProps.name());
                        sound = SoundEvents.BUCKET_EMPTY_LAVA;
                        break;
                    }
                }
            }
        } else if (heldStack.getItem() instanceof GemDustItem && !be.hasContent()) {
            result = EntityEventsUtils.fillBlockMoldWithGemDust(be, heldStack);
            sound = SoundEvents.SAND_PLACE;
        } else if (heldStack.isEmpty() && be.getContentType() == 2) {
            result = EntityEventsUtils.getGemDustFromBlockMold(player, be, heldStack);
            sound = SoundEvents.SAND_BREAK;
        }

        // Update block state after any interaction
        if (result && !level.isClientSide) {
            if (sound != null) {
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            BlockState currentState = level.getBlockState(pos);
            int content = ModUtils.getContentId(be.getContent());
            int shape = ModUtils.getBlockShapeId(be.getShape());

            if (currentState.getValue(CONTENT) != content || currentState.getValue(SHAPE) != shape) {
                level.setBlock(pos, currentState.setValue(CONTENT, content).setValue(SHAPE, shape), 3);
            }
        }

        return result;
    }
}
