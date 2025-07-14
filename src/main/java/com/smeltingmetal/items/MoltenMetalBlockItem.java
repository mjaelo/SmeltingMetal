package com.smeltingmetal.items;

import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a molten (still liquid) metal *block* output from furnaces/blasts.
 * Cannot be cooled directly; must be picked up with a Metal/Netherite Case block item.
 */
public class MoltenMetalBlockItem extends CoolingItem {

    public static final String METAL_ID_KEY = "MetalID";

    public MoltenMetalBlockItem(Properties props) {
        super(props);
    }

    public static ItemStack createStack(String metalId) {
        ItemStack st = new ItemStack(ModItems.MOLTEN_METAL_BLOCK.get());
        st.getOrCreateTag().putString(METAL_ID_KEY, metalId);
        return st;
    }

    public static @Nullable String getMetalId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(METAL_ID_KEY) ? tag.getString(METAL_ID_KEY) : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        String metalId = getMetalId(stack);
        if (metalId != null) {
            String idPath = metalId.contains(":") ? metalId.split(":")[1] : metalId;
            String formatted = idPath.substring(0, 1).toUpperCase() + idPath.substring(1);
            return Component.literal("Molten " + formatted + " Block");
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String metalId = getMetalId(stack);
        if (metalId != null) {
            tooltip.add(Component.translatable("tooltip." + SmeltingMetalMod.MODID + ".make_molten_metal_bucket"));
        }
    }


    @Override
    protected ItemStack getCooledItem(ItemStack stack, Level level, BlockPos pos) {
        String metalId = getMetalId(stack);
        if (metalId == null || !ModMetals.doesMetalExist(metalId)) {
            return null;
        }

        String[] parts = metalId.split(":", 2);
        String namespace = parts.length == 2 ? parts[0] : "minecraft";
        String path = parts.length == 2 ? parts[1] : parts[0];

        // First try to get raw item
        Item rawItem = CoolingItem.getItem(new ResourceLocation(namespace, "raw_" + path + "_block"));

        if (rawItem != null && rawItem != Items.AIR) {
            return new ItemStack(rawItem, stack.getCount());
        }
        return null;
    }

    @Override
    protected void onCooled(ItemStack originalStack, ItemStack cooledStack, Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

//    @Override
//    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
//        if (entity.level().isClientSide) return false;
//        // After 30 seconds (600 ticks) transform back into raw block
//        if (entity.getAge() > 600) {
//            String metalId = getMetalId(stack);
//            if (metalId != null) {
//                ResourceLocation metalLoc = ResourceLocation.tryParse(metalId);
//                if (metalLoc != null) {
//                    String namespace = metalLoc.getNamespace();
//                    String path = metalLoc.getPath();
//                    ResourceLocation rawBlockId = new ResourceLocation(namespace, "raw_" + path + "_block");
//                    Item rawBlock = ForgeRegistries.ITEMS.getValue(rawBlockId);
//                    if (rawBlock != null && rawBlock != Items.AIR) {
//                        entity.setItem(new ItemStack(rawBlock, stack.getCount()));
//                        entity.setPickUpDelay(10);
//                    }
//                }
//            }
//        }
//        return false;
//    }
}
