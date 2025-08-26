package com.smeltingmetal.items;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.mold.ItemMold;
import com.smeltingmetal.items.molten.MoltenMetalBlockItem;
import com.smeltingmetal.items.molten.MoltenMetalBucket;
import com.smeltingmetal.items.molten.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.smeltingmetal.SmeltingMetalMod.LOGGER;

public class ServerEventsUtils {
    public static void handleInventoryMoltenMetal(Player player) {
        Level level = player.level();
        if (level.isClientSide()) return;
        for (ItemStack itemStack : player.getInventory().items) {
            Class<?> containerClass = getContainerClass(itemStack);
            if (containerClass == null) continue; // not molten item
            List<ItemStack> containerStacks = findInInventory(player, containerClass);
            String metalType = ModMetals.getMetalTypeFromStack(itemStack);
            MetalProperties metalProps = ModMetals.getMetalProperties(metalType);

            if (containerStacks.isEmpty() || metalProps == null) {
                dropMoltenItem(player, itemStack, level);
                player.hurt(player.damageSources().lava(), 4f);
            } else {
                fillContainer(metalProps, player, containerStacks.get(0));
                itemStack.shrink(1);
            }
        }
    }

    public static List<ItemStack> findInInventory(Player player, Class<?> itemClass) {
        return player.getInventory().items.stream()
                .filter(stack -> itemClass.isInstance(stack.getItem())) // check if filled is true
                .filter(stack -> !(stack.getItem() instanceof ItemMold) || stack.getOrCreateTag().getInt("smeltingmetal:filled") == 0)
                .filter(stack -> !(stack.getItem() instanceof BucketItem bucket) || bucket.getFluid() == Fluids.EMPTY)
                .toList();
    }

    public static Class<?> getContainerClass(ItemStack itemStack) {
        boolean isMoltenItem = itemStack.getItem() instanceof MoltenMetalItem;
        boolean isMoltenBlock = itemStack.getItem() instanceof MoltenMetalBlockItem;
        if (!isMoltenItem && !isMoltenBlock) return null;
        return isMoltenItem ? ItemMold.class : BucketItem.class;
    }

    public static void fillContainer(MetalProperties metalProps, Player player, ItemStack containerStack) {
        ItemStack newItemStack;
        if (containerStack.getItem() instanceof ItemMold) {
            newItemStack = containerStack.copy();
            ModMetals.setMetalTypeToStack(newItemStack, metalProps.name());
        } else if (containerStack.getItem() instanceof BucketItem) {
            ResourceLocation bucketId = metalProps.bucket();
            MoltenMetalBucket bucketItem = (MoltenMetalBucket) ForgeRegistries.ITEMS.getValue(bucketId);
            if (bucketItem == null) return; // should not happen
            newItemStack = new ItemStack(bucketItem);
            ModMetals.setMetalTypeToStack(newItemStack, metalProps.name());
        } else {
            LOGGER.error("Invalid container item: " + containerStack.getItem());
            return;
        }
        containerStack.shrink(1);
        player.getInventory().add(newItemStack);
    }

    public static void handleIteractionCooling(PlayerInteractEvent.@NotNull RightClickBlock event, ItemStack heldItemStack) {
        if (!(heldItemStack.getItem() instanceof ItemMold heldItem
                && !Objects.equals(ModMetals.getMetalTypeFromStack(heldItemStack), ModMetals.DEFAULT_METAL_TYPE)
                && heldItem.getMaterialType() != MaterialType.CLAY)) return;

        // check if block is water
        Level level = event.getLevel();
        BlockPos pos = event.getPos().above();
        boolean isWaterBlock = level.getBlockState(pos).is(Blocks.WATER);
        boolean isWaterFluid = level.getFluidState(pos).is(FluidTags.WATER);
        if (!isWaterBlock && !isWaterFluid) return;

        // get metal properties
        MetalProperties metalProps = ModMetals.getMetalPropertiesFromStack(heldItemStack);
        if (metalProps == null) return;
        ResourceLocation ingotId = metalProps.ingot();
        Item ingotItem = ForgeRegistries.ITEMS.getValue(ingotId);
        if (ingotItem == null) return;

        // give ingot
        Player serverPlayer = event.getEntity();
        if (serverPlayer.getInventory().add(new ItemStack(ingotItem))) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.5F + level.random.nextFloat() * 0.5F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.5, 0.1, 0.5, 0.1);
            }
            if (heldItem.getMaterialType() != MaterialType.NETHERITE) {
                heldItemStack.shrink(1);
            } else {
                ModMetals.setMetalTypeToStack(heldItemStack, ModMetals.DEFAULT_METAL_TYPE);
            }
        }
        serverPlayer.drop(new ItemStack(ingotItem), false);
    }

    private static void dropMoltenItem(Player player, ItemStack stack, Level level) {
//        MetalProperties metalProperties = ModMetals.getMetalPropertiesFromStack(stack);
//
//        boolean hasLiquidForm = metalProperties.moltenBlock() != null;
//        if (Block.byItem(stack.getItem()) instanceof MoltenMetalBlock && hasLiquidForm) {
//            BlockPos pos = player.blockPosition().relative(player.getDirection());
//            var fluid = ForgeRegistries.FLUIDS.getValue(metalProperties.moltenBlock());
//            if (fluid != null) {
//                level.setBlockAndUpdate(pos, fluid.defaultFluidState().createLegacyBlock());
//                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
//            }
//        } else {
        ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
        item.setNoPickUpDelay();
        level.addFreshEntity(item);
//        }
        player.getInventory().removeItem(stack);
    }
}

