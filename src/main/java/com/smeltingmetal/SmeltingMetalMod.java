package com.smeltingmetal;

import com.smeltingmetal.config.ModConfig;
import com.smeltingmetal.init.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class for the Smelting Metal mod.
 * TODO gem processing.
 *      check grindstone compability with Spelunkery
 * TODO address lag when opening inventory
 * TODO add custom results by editing hardened and netherite molds directly
 *      heat up molds and then print items in them? right click lava?
 *      new molds: heated hardened and netherite molds? (reddened sides)
 *      after right clicking item with a heated mold, it becomes cooled?
 */
@Mod(SmeltingMetalMod.MODID)
public class SmeltingMetalMod {
    public static final String MODID = "smeltingmetal";
    public static final Logger LOGGER = LoggerFactory.getLogger(SmeltingMetalMod.class);

    private static RecipeManager recipeManager;
    private static MinecraftServer server;

    public SmeltingMetalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.CONFIG_SPEC);

        // Register event listeners
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::onConfigReloading);

        // Initialize mod components
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        // Register registries
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // Register recipes
        ModRecipes.register(modEventBus);

        // Register to the mod event bus for recipe handling
        modEventBus.addListener(this::onRegisterRecipes);

        // Register to Forge event bus for server lifecycle events
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoaded);
    }

    private void onRegisterRecipes(RegisterEvent event) {
        // This ensures our RecipeRemoval class is loaded and registered
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("SmeltingMetal mod setup complete"));
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
        if (event.getConfig().getSpec() == ModConfig.CONFIG_SPEC) {
            LOGGER.info("Reloading Smelting Metal config...");
            ModData.init();
        }
    }

    private void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModConfig.CONFIG_SPEC) {
            LOGGER.info("Config loaded, initializing metals");
            ModData.init();
        }
    }
}
