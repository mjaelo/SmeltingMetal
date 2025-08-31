package com.smeltingmetal.items;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.generic.MetalItem;
import com.smeltingmetal.items.mold.ItemMold;
import com.smeltingmetal.items.molten.MoltenMetalBlockItem;
import com.smeltingmetal.items.molten.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
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
            Item bucketItem = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
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

    public static void handleIteractionCooling(PlayerInteractEvent.@NotNull RightClickBlock event, ItemStack containerStack) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos().above();

        ItemStack cooledStack = getCooledMetalStack(containerStack, level, pos, false);
        if (cooledStack == null || cooledStack.isEmpty()) return;

        Player serverPlayer = event.getEntity();
        if (!serverPlayer.getInventory().add(cooledStack)) {
            serverPlayer.drop(cooledStack, false);
        }
    }

    public static ItemStack getCooledMetalStack(ItemStack metalStack, Level level, BlockPos pos, boolean shouldCool) {
        if (level.isClientSide()) return null;
        boolean inWater = level.getBlockState(pos).is(Blocks.WATER) || level.getFluidState(pos).is(FluidTags.WATER);
        if (!shouldCool && !inWater) return null;

        boolean isValidMold = metalStack.getItem() instanceof ItemMold moldItem
                && !Objects.equals(ModMetals.getMetalPropertiesFromStack(metalStack), ModMetals.DEFAULT_METAL_TYPE)
                && moldItem.getMaterialType() != MaterialType.CLAY;
        boolean isValidMoltenMetal = (metalStack.getItem() instanceof MoltenMetalItem || metalStack.getItem() instanceof MoltenMetalBlockItem)
                && !Objects.equals(ModMetals.getMetalPropertiesFromStack(metalStack), ModMetals.DEFAULT_METAL_TYPE);
        if (!isValidMold && !isValidMoltenMetal) return null;

        MetalProperties metalProps = ModMetals.getMetalPropertiesFromStack(metalStack);
        if (metalProps == null) return null;

        // get result item
        Item resultItem = isValidMold
                ? ForgeRegistries.ITEMS.getValue(metalProps.ingot())
                : ForgeRegistries.ITEMS.getValue(metalStack.getItem() instanceof MoltenMetalItem ? metalProps.raw() : metalProps.rawBlock());
        if (resultItem == null || resultItem == Items.AIR) return null;
        ItemStack resultStack = new ItemStack(resultItem);
        if (resultItem instanceof MetalItem) {
            ModMetals.setMetalTypeToStack(resultStack, metalProps.name());
        }

        // handle mold after cooling
        if (isValidMold) {
            if (((ItemMold) metalStack.getItem()).getMaterialType() != MaterialType.NETHERITE) {
                metalStack.shrink(1);
            } else {
                ModMetals.setMetalTypeToStack(metalStack, ModMetals.DEFAULT_METAL_TYPE);
            }
        }

        // play sound and particles
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.5F + level.random.nextFloat() * 0.5F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.5, 0.1, 0.5, 0.1);
        }

        return resultStack;
    }

    private static void dropMoltenItem(Player player, ItemStack stack, Level level) {
        MetalProperties metalProperties = ModMetals.getMetalPropertiesFromStack(stack);

        // if fluid is found, place it, otherwise drop it as an item
        if (stack.getItem() instanceof MoltenMetalBlockItem && metalProperties.moltenFluid() != null) {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(metalProperties.moltenFluid());
            BlockPos pos = player.blockPosition().relative(player.getDirection());
            level.setBlockAndUpdate(pos, fluid.defaultFluidState().createLegacyBlock());
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
            item.setNoPickUpDelay();
            level.addFreshEntity(item);
        }

        player.getInventory().removeItem(stack);
    }
}

