package com.smeltingmetal.utils;

import com.smeltingmetal.data.GemProperties;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.objects.mold.BlockMoldEntity;
import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.objects.molten.MoltenMetalBucket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityEventsUtils {

    protected static final int COOL_TICKS = 300; // 15 seconds

    public static boolean handleStackAutoCooling(ItemStack containerStack, ItemEntity entity) {
        Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        if (level.getBlockState(pos).is(Blocks.FIRE)) return false;
        boolean shouldCool = entity.getAge() >= COOL_TICKS;

        ItemStack cooledStack = ServerEventsUtils.getCooledContentStack(containerStack, level, pos, shouldCool);
        if (cooledStack == null || cooledStack.isEmpty()) return false;

        // Drop the cooled item
        if (containerStack.getItem() instanceof ItemMold && !containerStack.isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    containerStack);
            level.addFreshEntity(itemEntity);
        }
        entity.setItem(cooledStack);
        return true;
    }

    public static boolean fillBlockMold(Player player, BlockMoldEntity be, ItemStack heldStack, String content) {
        if (be.hasContent()) {
            return false;
        }
        be.setContent(content);

        // Replace the molten metal bucket with an empty bucket
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
        }

        return true;
    }

    public static boolean coolBlockMold(BlockPos pos, Player player, BlockMoldEntity be, ItemStack heldStack, Level level) {
        if (!be.hasContent()) return false;
        String content = be.getContent();
        MetalProperties metalProps = ModUtils.getMetalProperties(content);
        GemProperties gemProperties = ModUtils.getGemProperties(content);
        boolean isMetal = metalProps != null;
        if (metalProps == null && gemProperties == null) return false;

        // Create the metal block result
        String shape = be.getShape();
        Item resultItem = ForgeRegistries.ITEMS.getValue(
                ServerEventsUtils.getItemResult(
                        shape,
                        isMetal ? metalProps.blockResults() : gemProperties.blockResults(),
                        isMetal ? metalProps.block() : gemProperties.block())
        );
        if (resultItem == null || resultItem == Items.AIR) return false;
        ItemStack resultStack = new ItemStack(resultItem);

        // Drop the resulting item
        player.drop(resultStack, false);

        // Handle mold based on material type
        MaterialType moldMaterial = be.getMaterialType();
        if (moldMaterial == MaterialType.NETHERITE) {
            be.removeContent();
        } else {
            level.destroyBlock(pos, false);
        }

        // Replace water bucket with empty bucket
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            ItemStack emptyBucket = new ItemStack(Items.BUCKET);
            if (heldStack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, emptyBucket);
            } else if (!player.getInventory().add(emptyBucket)) {
                player.drop(emptyBucket, false);
            }
        }

        return true;
    }

    public static boolean fillBucketFromBlockMold(Player player, BlockMoldEntity be, ItemStack heldStack) {
        if (!be.hasContent()) return false;
        String metalType = be.getContent();
        MetalProperties metalProps = ModUtils.getMetalProperties(metalType);
        if (metalProps == null) return false;

        Item bucketItem = ForgeRegistries.ITEMS.getValue(metalProps.bucket());
        if (bucketItem == null) return false;
        ItemStack bucketStack = new ItemStack(bucketItem);
        if (bucketItem instanceof MoltenMetalBucket) {
            ModUtils.setContentToStack(bucketStack, metalType);
        }

        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
            if (heldStack.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, bucketStack);
            } else if (!player.getInventory().add(bucketStack)) {
                player.drop(bucketStack, false);
            }
        }

        // Empty the mold
        be.removeContent();
        return true;
    }

    public static boolean getGemDustFromBlockMold(Player player, BlockMoldEntity be, ItemStack heldStack) {
        String gemName = be.getContent();
        GemProperties gemProps = ModUtils.getGemProperties(gemName);
        if (gemProps == null) return false;

        Item dustItem = ModItems.GEM_DUST_ITEM.get();
        ItemStack resultStack = new ItemStack(dustItem, 9);
        ModUtils.setContentToStack(resultStack, gemName);

        if (heldStack.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, resultStack);
        } else if (!player.getInventory().add(resultStack)) {
            player.drop(resultStack, false);
        }

        be.removeContent();
        return true;
    }

    public static boolean fillBlockMoldWithGemDust(BlockMoldEntity be, ItemStack heldStack) {
        if (heldStack.getCount() < 9) return false;
        String gemName = ModUtils.getContentFromStack(heldStack);
        if (gemName == null || gemName.equals(ModData.DEFAULT_CONTENT)) return false;
        heldStack.shrink(9);
        be.setContent(gemName);
        return true;
    }
}
