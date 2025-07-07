package com.smeltingmetal;

import com.smeltingmetal.items.FilledMoldItem;
import com.smeltingmetal.items.FilledNetheriteMoldItem;
import com.smeltingmetal.items.MoltenMetalItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    public static final RegistryObject<Item> CLAY_MOLD = ITEMS.register("clay_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HARDENED_MOLD = ITEMS.register("hardened_mold",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NETHERITE_MOLD = ITEMS.register("netherite_mold",
            () -> new Item(new Item.Properties()));

    // Generic Molten Metal Item
    public static final RegistryObject<Item> MOLTEN_METAL = ITEMS.register("molten_metal",
            () -> new MoltenMetalItem(new Item.Properties().stacksTo(1)));

    // Filled mold items
    public static final RegistryObject<FilledMoldItem> FILLED_MOLD = ITEMS.register("filled_mold",
            () -> new FilledMoldItem(new Item.Properties()));

    public static final RegistryObject<FilledNetheriteMoldItem> FILLED_NETHERITE_MOLD = ITEMS.register("filled_netherite_mold",
            () -> new FilledNetheriteMoldItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

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
}