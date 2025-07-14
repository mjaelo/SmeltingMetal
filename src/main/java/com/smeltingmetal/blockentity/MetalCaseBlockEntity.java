package com.smeltingmetal.blockentity;

import com.smeltingmetal.ModBlockEntities;
import com.smeltingmetal.ModBlocks;
import com.smeltingmetal.ModItems;
import com.smeltingmetal.blocks.HardenedCaseBlock;
import com.smeltingmetal.items.MoltenMetalBucketItem;
import com.smeltingmetal.items.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Stores up to one MoltenMetalItem of the same metal-id.  On water contact when full
 * it converts to the corresponding metal-block.  Netherite variant will return the empty
 * case item to the world (handled by caller via flag).
 */
public class MetalCaseBlockEntity extends BlockEntity {

    private static final int CAPACITY = 1;
    private static final int BUCKET_VOLUME = 1;
    private final ItemStackHandler handler = new ItemStackHandler(CAPACITY) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false; // direct item insert disabled
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide) {
                int fill = getFill();
                BlockState st = level.getBlockState(worldPosition);
                if (st.hasProperty(HardenedCaseBlock.FILL)) {
                    level.setBlock(worldPosition, st.setValue(HardenedCaseBlock.FILL, fill), 3);
                }
            }
        }
    };

    private final LazyOptional<ItemStackHandler> handlerCap = LazyOptional.of(() -> handler);

    private String metalType; // null until first insertion

    public MetalCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.hardened_case.get(), pos, state);
    }

    /* ------------- Public helpers --------------- */
    public int getFill() {
        return handler.getStackInSlot(0).getCount();
    }

    public boolean insertMolten(ItemStack stack){return false;}

    /**
     * Player right-click interaction from the block class.
     * Handles filling from molds and immediate water cooling.
     */
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, ItemStack held, boolean returnsCase) {
        Level lvl = getLevel();
        if (lvl == null) return false;

        // 1. Water bucket cooling (takes priority)
        if ((held.getItem() == Items.WATER_BUCKET) && getFill() == 1) {
            // consume water bucket (unless creative)
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(empty)) player.drop(empty, false);
            }

            performCooling(returnsCase);
            return true;
        }

        // Molten bucket pouring
        if (held.getItem() instanceof MoltenMetalBucketItem) {
            String id = MoltenMetalBucketItem.getMetalId(held);
            if (id == null) return false;
            if (metalType == null) metalType = id;
            if (!id.equals(metalType)) return false;

            if (getFill()==1) return false;
            ItemStack molten = MoltenMetalItem.createStack(id);
            molten.setCount(1);
            handler.setStackInSlot(0, molten);
            int inserted = 1;
            if (!player.getAbilities().instabuild) {
                // consume bucket and give empty one
                held.shrink(1);
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.getInventory().add(empty)) player.drop(empty, false);
            }
            // sound
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    private void performCooling(boolean returnsCase) {
        Level level1 = getLevel();
        if (level1 == null) return;

        // determine block to set (<metal>_block) fallback iron_block
        String[] parts = metalType.split(":",2);
        String ns = parts.length==2?parts[0]:"minecraft";
        String path = parts.length==2?parts[1]:parts[0];
        ResourceLocation outId = new ResourceLocation(ns, path + "_block");
        Block outBlock = ForgeRegistries.BLOCKS.getValue(outId);
        if (outBlock == null || outBlock == Blocks.AIR) outBlock = Blocks.IRON_BLOCK;

        // Replace block
        level1.setBlock(worldPosition, outBlock.defaultBlockState(), 3);

        // Drop empty case if needed
        if (returnsCase) {
            ItemStack emptyCase = new ItemStack(this.getBlockState().getBlock() == ModBlocks.NETHERITE_CASE.get() ? ModItems.NETHERITE_CASE.get() : ModItems.HARDENED_CASE.get());
            ItemEntity e = new ItemEntity(level1, worldPosition.getX()+0.5, worldPosition.getY()+1, worldPosition.getZ()+0.5, emptyCase);
            level1.addFreshEntity(e);
        }

        level1.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,1f,1f);
        if (level1 instanceof ServerLevel slvl) {
            slvl.sendParticles(ParticleTypes.CLOUD, worldPosition.getX()+0.5, worldPosition.getY()+1, worldPosition.getZ()+0.5, 8,0.3,0.1,0.3,0.01);
        }
    }

    /* ------------- Conversion Logic -------------- */
    private void tryCooling(boolean returnsCase) {
        if (getFill() < 1 || metalType == null) return;
        if (!isWaterContact()) return;

        // Determine block output: <metal>_block fallback to iron_block
        String[] parts = metalType.split(":", 2);
        String ns = parts.length == 2 ? parts[0] : "minecraft";
        String path = parts.length == 2 ? parts[1] : parts[0];
        ResourceLocation blockId = new ResourceLocation(ns, path + "_block");
        Block outBlock = ForgeRegistries.BLOCKS.getValue(blockId);
        if (outBlock == null || outBlock == Blocks.AIR) outBlock = Blocks.IRON_BLOCK;

        Level lvl = getLevel();
        if (lvl == null) return;
        lvl.setBlock(worldPosition, outBlock.defaultBlockState(), 3);
        if (returnsCase) {
            ItemStack empty = new ItemStack(this.getBlockState().getBlock() == ModBlocks.NETHERITE_CASE.get() ?
                    ModItems.NETHERITE_CASE.get() : ModItems.HARDENED_CASE.get());
            ItemEntity e = new ItemEntity(lvl, worldPosition.getX()+0.5, worldPosition.getY()+1.0, worldPosition.getZ()+0.5, empty);
            lvl.addFreshEntity(e);
        }
        lvl.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
    }

    private boolean isWaterContact() {
        return level.getFluidState(worldPosition.above()).is(FluidTags.WATER) ||
               level.getFluidState(worldPosition).is(FluidTags.WATER);
    }

    /* ------------- Tick -------------------------- */
    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (!(blockEntity instanceof MetalCaseBlockEntity be)) return;
        if (level.isClientSide) return;

        // Absorb nearby molten metal entities if not full
        if (be.getFill() < CAPACITY) {
            List<ItemEntity> list = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
            for (ItemEntity it : list) {
                ItemStack stack = it.getItem();
                if (be.insertMolten(stack)) {
                    if (stack.isEmpty()) it.discard();
                }
            }
        }
        // Cooling check
        be.tryCooling(be.getBlockState().getBlock() == ModBlocks.NETHERITE_CASE.get());
    }

    /* ------------- Capability / NBT -------------- */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return handlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", handler.serializeNBT());
        if (metalType != null) tag.putString("metal", metalType);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        handler.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("metal")) metalType = tag.getString("metal");
    }

    public static BlockEntityTicker<MetalCaseBlockEntity> getTicker() {
        return MetalCaseBlockEntity::tick;
    }
}
