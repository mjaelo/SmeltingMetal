package com.smeltingmetal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.recipes.ModRecipes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SmeltingMetalMod.MODID)
public class SmeltingMetalMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "smeltingmetal";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(SmeltingMetalMod.class);

    private static RecipeManager recipeManager;
    private static MinecraftServer server;

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
        
        // Register to Forge event bus for server lifecycle events
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    private void onRegisterRecipes(RegisterEvent event) {
        // This ensures our RecipeRemoval class is loaded and registered
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("SmeltingMetal mod setup complete");
        });
    }
    

    public static RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    public static MinecraftServer getServer() {
        return server;
    }

    public static void setRecipeManager(RecipeManager recipeManager) {
        SmeltingMetalMod.recipeManager = recipeManager;
    }

    public static void setServer(MinecraftServer server) {
        SmeltingMetalMod.server = server;
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == MetalsConfig.CONFIG_SPEC) {
            LOGGER.info("Reloading metal configurations...");
            ModMetals.init();
        }
    }
}
