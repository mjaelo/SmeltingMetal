package com.smeltingmetal;

import com.smeltingmetal.recipes.ModRecipes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmeltingMetalMod.MODID)
public class SmeltingMetalMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "smeltingmetal";

    public SmeltingMetalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register custom items
        ModItems.register(modEventBus);
        ModRecipes.register(modEventBus);

        // Register event listeners for Forge's common event bus (player interactions)
        MinecraftForge.EVENT_BUS.register(this); // For this class if it has events
        MinecraftForge.EVENT_BUS.register(new ModEvents()); // For your dedicated event handler class
    }
}
