package com.smeltingmetal.items.metalBlock;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.CoolingItem;
import com.smeltingmetal.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

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
            return Component.translatable("item." + SmeltingMetalMod.MODID + ".molten_metal_block", formatted);
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

        // Get the metal properties to access the raw block ID
        return ModMetals.getMetalProperties(metalId)
                .map(metalProps -> {
                    // Use the raw block ID from MetalProperties
                    Item rawBlockItem = ForgeRegistries.ITEMS.getValue(metalProps.rawBlockId());
                    if (rawBlockItem != null && rawBlockItem != Items.AIR) {
                        return new ItemStack(rawBlockItem, stack.getCount());
                    }
                    return null;
                })
                .orElse(null);
    }

}
