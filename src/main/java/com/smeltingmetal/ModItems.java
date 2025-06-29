package com.smeltingmetal;

import com.smeltingmetal.items.FilledMoldItem;
import com.smeltingmetal.items.MoltenMetalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    public static final RegistryObject<Item> CLAY_MOLD = ITEMS.register("clay_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HARDENED_MOLD = ITEMS.register("hardened_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<MoltenMetalItem> MOLTEN_METAL = ITEMS.register("molten_metal",
            () -> new MoltenMetalItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<FilledMoldItem> FILLED_MOLD = ITEMS.register("filled_mold",
            () -> new FilledMoldItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        eventBus.addListener(ModItems::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CLAY_MOLD);
            event.accept(HARDENED_MOLD);
            event.accept(MOLTEN_METAL);
            event.accept(FILLED_MOLD);
        }
    }
}