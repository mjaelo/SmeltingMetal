package com.smeltingmetal.recipes;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import static com.mojang.text2speech.Narrator.LOGGER;

/**
 * Reload listener that re-processes and replaces recipes whenever server datapacks are (re)loaded.
 */
public class RecipeReloadListener extends SimplePreparableReloadListener<Void> {

    private final RegistryAccess registryAccess;

    public RecipeReloadListener(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    @Override
    protected @Nullable Void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        // No asynchronous preparation is needed – all work is done synchronously in apply().
        return null;
    }

    @Override
    protected void apply(@Nullable Void object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        LOGGER.info("RecipeReloadListener → running replacements");
        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null && SmeltingMetalMod.getServer() != null) {
            recipeManager = SmeltingMetalMod.getServer().getRecipeManager();
        }

        if (recipeManager != null) {
            RecipeProcessor.process(recipeManager, registryAccess);
        }
    }
}
