package com.smeltingmetal.items;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Generic bucket containing molten metal. Stores the metal id in NBT (key MetalID).
 * Cannot be placed; only usable with Metal/Netherite Case blocks.
 */
public class MoltenMetalBucketItem extends Item {
    public static final String TAG_METAL_ID = "MetalID";

    public MoltenMetalBucketItem(Properties props) {
        super(props);
    }

    public static ItemStack createStack(String metalId) {
        ItemStack stack = new ItemStack(com.smeltingmetal.ModItems.MOLTEN_METAL_BUCKET.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_METAL_ID, metalId);
        return stack;
    }

    @Nullable
    public static String getMetalId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_METAL_ID)) return null;
        return tag.getString(TAG_METAL_ID);
    }

    @Override
    public Component getName(ItemStack stack) {
        String metalId = getMetalId(stack);
        if (metalId != null) {
            return Component.translatable("item." + SmeltingMetalMod.MODID + ".molten_metal_bucket", metalId);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String metalId = getMetalId(stack);
        if (metalId != null) {
            tooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".molten_bucket_metal", metalId));
        }
        tooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".molten_bucket_use"));
    }
}
