package com.smeltingmetal.init;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.objects.gem.GemDustItem;
import com.smeltingmetal.objects.molten.MoltenMetalBucket;
import com.smeltingmetal.objects.molten.MoltenMetalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

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
            event.accept(GEM_DUST_ITEM);
            event.accept(MOLTEN_METAL_ITEM);
            event.accept(MOLTEN_METAL_BUCKET);
            event.accept(ModBlocks.MOLTEN_METAL_BLOCK_ITEM.get());
            event.accept(ModBlocks.GEM_DUST_BLOCK_ITEM.get());
        }
    }

    public static Map<String, RegistryObject<Item>> ITEM_MOLDS_CLAY = registerItemMoldShapes();

    public static Map<String, RegistryObject<Item>> registerItemMoldShapes() {
        Map<String, RegistryObject<Item>> itemMolds = new HashMap<>();
        for (String shape : ModMetals.DEFAULT_ITEM_SHAPES) {
            itemMolds.put(shape, ITEMS.register("item_mold_empty/clay_" + shape,
                    () -> new ItemMold(new Item.Properties(), MaterialType.CLAY, shape)));
        }
        return itemMolds;
    }


    // Mold items
    public static final RegistryObject<Item> ITEM_MOLD_CLAY = ITEM_MOLDS_CLAY.get(ModMetals.DEFAULT_ITEM_SHAPE);
    
    public static final RegistryObject<Item> ITEM_MOLD_HARDENED = ITEMS.register("item_mold_hardened",
            () -> new ItemMold(new Item.Properties(),  MaterialType.HARDENED));
    
    public static final RegistryObject<Item> ITEM_MOLD_NETHERITE = ITEMS.register("item_mold_netherite",
            () -> new ItemMold(new Item.Properties(),  MaterialType.NETHERITE));

    // Molten metal items
    public static final RegistryObject<Item> MOLTEN_METAL_ITEM = ITEMS.register("molten_metal_item",
            () -> new MoltenMetalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MOLTEN_METAL_BUCKET = ITEMS.register("molten_metal_bucket",
            () -> new MoltenMetalBucket(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GEM_DUST_ITEM = ITEMS.register("gem_dust_item",
            () -> new GemDustItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addItemsToCreativeTab);
    }
}