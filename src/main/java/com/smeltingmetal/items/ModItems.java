package com.smeltingmetal.items;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.blocks.ModBlocks;
import com.smeltingmetal.items.metalBlock.MoltenMetalBlockItem;
import com.smeltingmetal.items.metalBlock.MoltenMetalBucketItem;
import com.smeltingmetal.items.metalIngot.FilledMoldItem;
import com.smeltingmetal.items.metalIngot.FilledNetheriteMoldItem;
import com.smeltingmetal.items.metalIngot.MoltenMetalItem;
import net.minecraft.world.item.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handles registration and management of all custom items in the Smelting Metal mod.
 * This includes molds, molten metals, and other special items used in the metal smelting process.
 */
@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    // Molten Metal Item
    public static final RegistryObject<Item> CLAY_MOLD = ITEMS.register("clay_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HARDENED_MOLD = ITEMS.register("hardened_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NETHERITE_MOLD = ITEMS.register("netherite_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOLTEN_METAL = ITEMS.register("molten_metal",
            () -> new MoltenMetalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<FilledMoldItem> FILLED_MOLD = ITEMS.register("filled_mold",
            () -> new FilledMoldItem(new Item.Properties()));

    public static final RegistryObject<FilledNetheriteMoldItem> FILLED_NETHERITE_MOLD = ITEMS.register("filled_netherite_mold",
            () -> new FilledNetheriteMoldItem(new Item.Properties()));

    // Molten Metal Block
    public static final RegistryObject<Item> CLAY_CASE = ITEMS.register("clay_case",
            () -> new BlockItem(ModBlocks.CLAY_CASE.get(), new Item.Properties()));

    public static final RegistryObject<Item> HARDENED_CASE = ITEMS.register("hardened_case",
            () -> new BlockItem(ModBlocks.HARDENED_CASE.get(), new Item.Properties()));

    public static final RegistryObject<Item> NETHERITE_CASE = ITEMS.register("netherite_case",
            () -> new BlockItem(ModBlocks.NETHERITE_CASE.get(), new Item.Properties()));

    public static final RegistryObject<Item> MOLTEN_METAL_BLOCK = ITEMS.register("molten_metal_block",
                () -> new MoltenMetalBlockItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOLTEN_METAL_BUCKET = ITEMS.register("molten_metal_bucket",
            () -> new MoltenMetalBucketItem(new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items to the Ingredients tab
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CLAY_MOLD);
            event.accept(HARDENED_MOLD);
            event.accept(NETHERITE_MOLD);
        }
    }

    public static ItemStack getMoltenMetalStack(String metalId) {
        return MoltenMetalItem.createStack(metalId);
    }

    public static ItemStack getMoltenMetalBlockStack(String metalId) {
        return MoltenMetalBlockItem.createStack(metalId);
    }
}