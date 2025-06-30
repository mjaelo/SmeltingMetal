package com.smeltingmetal;

import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.recipes.ModRecipes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MetalsConfig.CONFIG_SPEC);
        
        // Register event listeners
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::onConfigReloading);
        
        // Register items and recipes
        ModItems.ITEMS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        
        // Initialize metals from config
        ModMetals.init();
        
        // Register to the mod event bus for recipe handling
        modEventBus.addListener(this::onRegisterRecipes);
    }
    
    private void onRegisterRecipes(RegisterEvent event) {
        // This ensures our RecipeRemoval class is loaded and registered
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        // Any common setup code can go here
        LOGGER.info("SmeltingMetal mod setup complete");
    }
    
    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == MetalsConfig.CONFIG_SPEC) {
            LOGGER.info("Reloading metal configurations...");
            ModMetals.init();
        }
    }
}
