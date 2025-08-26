package com.smeltingmetal.init;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.items.mold.ItemMold;
import com.smeltingmetal.items.molten.MoltenMetalBucket;
import com.smeltingmetal.items.molten.MoltenMetalItem;
import com.smeltingmetal.items.raw.MetalNugget;
import com.smeltingmetal.items.raw.RawMetalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    // Method to add items to the Ingredients tab
    public static void addItemsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            // Add item molds
            event.accept(ITEM_MOLD_CLAY);
            event.accept(ITEM_MOLD_HARDENED);
            event.accept(ITEM_MOLD_NETHERITE);
            event.accept(ModBlocks.BLOCK_MOLD_CLAY_ITEM.get());
            event.accept(ModBlocks.BLOCK_MOLD_HARDENED_ITEM.get());
            event.accept(ModBlocks.BLOCK_MOLD_NETHERITE_ITEM.get());
            // add generic metal items
            event.accept(MOLTEN_METAL_ITEM);
            event.accept(MOLTEN_METAL_BUCKET);
            event.accept(ModBlocks.MOLTEN_METAL_BLOCK_ITEM.get());
            event.accept(METAL_NUGGET);
            event.accept(RAW_METAL_ITEM);
            event.accept(ModBlocks.RAW_METAL_BLOCK_ITEM.get());
        }
    }

    // Mold items
    public static final RegistryObject<Item> ITEM_MOLD_CLAY = ITEMS.register("item_mold_clay",
            () -> new ItemMold(new Item.Properties(),  MaterialType.CLAY));
    
    public static final RegistryObject<Item> ITEM_MOLD_HARDENED = ITEMS.register("item_mold_hardened",
            () -> new ItemMold(new Item.Properties(),  MaterialType.HARDENED));
    
    public static final RegistryObject<Item> ITEM_MOLD_NETHERITE = ITEMS.register("item_mold_netherite",
            () -> new ItemMold(new Item.Properties(),  MaterialType.NETHERITE));

    // Molten metal items
    public static final RegistryObject<Item> MOLTEN_METAL_ITEM = ITEMS.register("molten_metal_item",
            () -> new MoltenMetalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOLTEN_METAL_BUCKET = ITEMS.register("molten_metal_bucket",
            () -> new MoltenMetalBucket(new Item.Properties().stacksTo(1)));

    // Raw metal items
    public static final RegistryObject<Item> METAL_NUGGET = ITEMS.register("metal_nugget",
            () -> new MetalNugget(new Item.Properties()));

    public static final RegistryObject<Item> RAW_METAL_ITEM = ITEMS.register("raw_metal",
            () -> new RawMetalItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addItemsToCreativeTab);
    }
}