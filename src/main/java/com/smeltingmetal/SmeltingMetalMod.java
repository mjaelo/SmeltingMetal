package com.smeltingmetal;

import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.recipes.ModRecipes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.mojang.text2speech.Narrator.LOGGER;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmeltingMetalMod.MODID)
public class SmeltingMetalMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "smeltingmetal";

    public SmeltingMetalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        MetalsConfig.register();
        
        // Initialize metals from config
        ModMetals.init();

        // Register custom items and recipes
        ModItems.register(modEventBus);
        ModRecipes.register(modEventBus);

        // Register event listeners
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ModEvents());
        
        // Register config change listener
        modEventBus.addListener(this::onConfigReloading);
    }
    
    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == MetalsConfig.CONFIG_SPEC) {
            LOGGER.info("Reloading metal configurations...");
            ModMetals.init();
        }
    }
}
