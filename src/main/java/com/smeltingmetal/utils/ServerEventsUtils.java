package com.smeltingmetal.utils;

import com.smeltingmetal.config.ModConfig;
import com.smeltingmetal.data.GemProperties;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModBlocks;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.objects.gem.GemDustItem;
import com.smeltingmetal.objects.generic.MetalItem;
import com.smeltingmetal.objects.mold.BlockMoldItem;
import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.objects.molten.MoltenMetalBlockItem;
import com.smeltingmetal.objects.molten.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
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
import java.util.Map;

import static com.smeltingmetal.SmeltingMetalMod.LOGGER;

public class ServerEventsUtils {
    public static void checkInventoryForMoltenMetal(Player player) {
        Level level = player.level();
        if (level.isClientSide()) return;
        for (ItemStack itemStack : player.getInventory().items) {
            Class<?> containerClass = getContainerClass(itemStack);
            if (containerClass == null) continue; // not molten item
            List<ItemStack> containerStacks = findInInventory(player, containerClass);
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(itemStack);

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
                .filter(stack -> itemClass.isInstance(stack.getItem()))
                .filter(stack ->
                        (stack.getItem() instanceof ItemMold itemMold && itemMold.getMaterialType() != MaterialType.CLAY && ModUtils.getContentFromStack(stack).equals(ModData.DEFAULT_CONTENT))
                                || (stack.getItem() instanceof BucketItem bucket && bucket.getFluid() == Fluids.EMPTY)
                )
                .toList();
    }

    public static Class<?> getContainerClass(ItemStack itemStack) {
        boolean isMoltenItem = itemStack.getItem() instanceof MoltenMetalItem;
        boolean isMoltenBlock = itemStack.getItem() instanceof MoltenMetalBlockItem;
        if (!isMoltenItem && !isMoltenBlock) return
                null;
        return isMoltenItem ? ItemMold.class : BucketItem.class;
    }

    public static void fillContainer(MetalProperties metalProps, Player player, ItemStack containerStack) {
        ItemStack newItemStack;
        if (containerStack.getItem() instanceof ItemMold itemMold && itemMold.getMaterialType() != MaterialType.CLAY) {
            newItemStack = new ItemStack(containerStack.getItem(), 1);
            ModUtils.setContentToStack(newItemStack, metalProps.name());
            ModUtils.setShapeToStack(newItemStack, ModUtils.getShapeFromStack(containerStack), false);
        } else if (containerStack.getItem() instanceof BucketItem) {
            Item bucketItem = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
            if (bucketItem == null) return; // should not happen
            newItemStack = new ItemStack(bucketItem);
            ModUtils.setContentToStack(newItemStack, metalProps.name());
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

        ItemStack cooledStack = getCooledContentStack(containerStack, level, pos, false);
        if (cooledStack == null || cooledStack.isEmpty()) return;

        Player serverPlayer = event.getEntity();
        if (!serverPlayer.getInventory().add(cooledStack)) {
            serverPlayer.drop(cooledStack, false);
        }
    }

    public static ItemStack getCooledContentStack(ItemStack contentStack, Level level, BlockPos pos, boolean shouldCool) {
        if (level.isClientSide()) return null;
        MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(contentStack);
        GemProperties gemProperties = ModUtils.getGemPropertiesFromStack(contentStack);
        boolean isMetal = metalProps != null;
        if (metalProps == null && gemProperties == null) return null;

        boolean inFluid = level.getBlockState(pos).is(isMetal ? Blocks.WATER : Blocks.LAVA) || level.getFluidState(pos).is(isMetal ? FluidTags.WATER : FluidTags.LAVA);
        if (!shouldCool && !inFluid) return null;

        boolean isValidMold = contentStack.getItem() instanceof ItemMold moldItem && moldItem.getMaterialType() != MaterialType.CLAY;
        boolean isValidMoltenMetal = isMetal && (contentStack.getItem() instanceof MoltenMetalItem || contentStack.getItem() instanceof MoltenMetalBlockItem);
        if (!isValidMold && !isValidMoltenMetal) return null;

        // get result item
        String shape = ModUtils.getShapeFromStack(contentStack);
        Item resultItem = ForgeRegistries.ITEMS.getValue(isValidMold
                ? getItemResult(
                        shape,
                        isMetal ? metalProps.itemResults(): gemProperties.itemResults(),
                        isMetal ? metalProps.ingot(): gemProperties.gem())
                : getRawFromMetalProp(contentStack, metalProps));
        if (resultItem == null || resultItem == Items.AIR) return null;
        ItemStack resultStack = new ItemStack(resultItem);
        if (resultItem instanceof MetalItem) {
            ModUtils.setContentToStack(resultStack, isMetal? metalProps.name() : gemProperties.name());
        }

        // handle mold after cooling
        if (isValidMold) {
            if (((ItemMold) contentStack.getItem()).getMaterialType() != MaterialType.NETHERITE) {
                contentStack.shrink(1);
            } else {
                ModUtils.setContentToStack(contentStack, ModData.DEFAULT_CONTENT);
            }
        }

        // play sound and particles
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.5F + level.random.nextFloat() * 0.5F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.5, 0.1, 0.5, 0.1);
        }

        return resultStack;
    }

    public static boolean pourContentBetweenItemMolds(ItemStack mainHand, ItemStack offHand, Player player) {
        // Check if both items are molds
        if (!(mainHand.getItem() instanceof ItemMold && offHand.getItem() instanceof ItemMold)) {
            return false;
        }

        String mainHandContent = ModUtils.getContentFromStack(mainHand);
        String offHandContent = ModUtils.getContentFromStack(offHand);
        boolean isMainHandMoldEmpty = mainHandContent.equals(ModData.DEFAULT_CONTENT);
        boolean isOffHandMoldEmpty = offHandContent.equals(ModData.DEFAULT_CONTENT);

        // Only proceed if one is empty and the other is not
        if (isMainHandMoldEmpty == isOffHandMoldEmpty) return false;

        // Update the metal types
        ModUtils.setContentToStack(mainHand, offHandContent);
        ModUtils.setContentToStack(offHand, mainHandContent);

        // Update the player's hands
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand.copy());
        player.setItemInHand(InteractionHand.OFF_HAND, offHand.copy());
        return true;
    }

    public static boolean printItemIntoItemMold(ItemStack mainHand, ItemStack offHand, Player player) {
        // Check which hand is the mold
        boolean isMainHandMold = mainHand.getItem() instanceof ItemMold && ModUtils.getContentFromStack(mainHand).equals(ModData.DEFAULT_CONTENT);
        boolean isOffHandMold = offHand.getItem() instanceof ItemMold && ModUtils.getContentFromStack(offHand).equals(ModData.DEFAULT_CONTENT);
        if (!isMainHandMold && !isOffHandMold) return false;

        String handShape = ModUtils.getShapeKeyFromString(isMainHandMold ? offHand.getDescriptionId() : mainHand.getDescriptionId(), false);
        if (ModConfig.CONFIG.blockKeywords.get().stream().anyMatch(handShape::contains)) return false;
        Item newItem = ModItems.ITEM_MOLDS_CLAY.get(handShape).get();
        ItemStack newMainHand = isMainHandMold ? new ItemStack(newItem, mainHand.getCount()) : mainHand.copy();
        ItemStack newoffHand = isOffHandMold ? new ItemStack(newItem, offHand.getCount()) : offHand.copy();
        ModUtils.setShapeToStack(isMainHandMold ? newMainHand : newoffHand, handShape, false);

        // Update the player's hands
        player.setItemInHand(InteractionHand.MAIN_HAND, newMainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, newoffHand);
        return true;
    }

    public static boolean printItemIntoBlockMold(ItemStack mainHand, ItemStack offHand, Player player) {
        // Check which hand is the mold
        boolean isMainHandMold = mainHand.getItem() instanceof BlockMoldItem mold && ModUtils.getContentFromStack(mainHand).equals(ModData.DEFAULT_CONTENT) && mold.getMaterialType() == MaterialType.CLAY;
        boolean isOffHandMold = offHand.getItem() instanceof BlockMoldItem mold && ModUtils.getContentFromStack(offHand).equals(ModData.DEFAULT_CONTENT) && mold.getMaterialType() == MaterialType.CLAY;
        if (!isMainHandMold && !isOffHandMold) return false;

        String handShape = ModUtils.getShapeKeyFromString(isMainHandMold ? offHand.getDescriptionId() : mainHand.getDescriptionId(), true);
        if (ModConfig.CONFIG.blockKeywords.get().stream().anyMatch(handShape::contains)) return false;
        Item newItem = ModBlocks.BLOCK_MOLDS_CLAY.get(handShape).get();
        ItemStack newMainHand = isMainHandMold ? new ItemStack(newItem, mainHand.getCount()) : mainHand.copy();
        ItemStack newoffHand = isOffHandMold ? new ItemStack(newItem, offHand.getCount()) : offHand.copy();
        ModUtils.setShapeToStack(isMainHandMold ? newMainHand : newoffHand, handShape, true);

        // Update the player's hands
        player.setItemInHand(InteractionHand.MAIN_HAND, newMainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, newoffHand);
        return true;
    }

    public static boolean putGemDustIntoMold(ItemStack mainHand, ItemStack offHand, Player player) {
        // Check which hand is the mold
        boolean isMainHandMold = (mainHand.getItem() instanceof ItemMold || mainHand.getItem() instanceof BlockMoldItem) && ModUtils.getContentFromStack(mainHand).equals(ModData.DEFAULT_CONTENT);
        boolean isOffHandMold = (offHand.getItem() instanceof ItemMold || offHand.getItem() instanceof BlockMoldItem) && ModUtils.getContentFromStack(offHand).equals(ModData.DEFAULT_CONTENT);
        if (isMainHandMold == isOffHandMold) return false;
        boolean isMainHandGemDust = mainHand.getItem() instanceof GemDustItem;
        boolean isOffHandGemDust = offHand.getItem() instanceof GemDustItem;
        if (isMainHandGemDust == isOffHandGemDust) return false;

        String gemName = isMainHandGemDust ? ModUtils.getContentFromStack(mainHand) : ModUtils.getContentFromStack(offHand);
        if (gemName.equals(ModData.DEFAULT_CONTENT)) return false;

        int requiredCount = isMainHandMold
                ? mainHand.getItem() instanceof ItemMold ? 1 : 9
                : offHand.getItem() instanceof ItemMold ? 1 : 9;
        if (isMainHandGemDust
                ? mainHand.getCount() < requiredCount
                : offHand.getCount() < requiredCount) return false;
        (isMainHandGemDust ? mainHand : offHand).shrink(requiredCount);
        ModUtils.setContentToStack(isMainHandMold ? mainHand : offHand, gemName);

        // Update the player's hands
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        return true;
    }

    private static ResourceLocation getRawFromMetalProp(ItemStack metalStack, MetalProperties metalProps) {
        return metalStack.getItem() instanceof MoltenMetalItem ? metalProps.raw() : metalProps.rawBlock();
    }

    public static ResourceLocation getItemResult(String shape, Map<String, ResourceLocation> itemResults, ResourceLocation defaultResult) {
        ResourceLocation result = itemResults.get(shape);
        return result != null ? result : defaultResult;
    }

    private static void dropMoltenItem(Player player, ItemStack stack, Level level) {
        MetalProperties metalProperties = ModUtils.getMetalPropertiesFromStack(stack);

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

