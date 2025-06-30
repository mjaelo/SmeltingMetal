package com.smeltingmetal.recipes;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.ModMetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class RecipeRemoval {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> RECIPES_TO_REMOVE = new HashSet<>();
    private static boolean initialized = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            // Get all metals from config
            Map<String, String> metalToIngotMap = ModMetals.getAllMetalProperties().values().stream()
                    .collect(Collectors.toMap(
                            metal -> metal.id(),
                            metal -> metal.ingotId().toString()
                    ));

            // Get the recipe manager
            RecipeManager recipeManager = event.getServer().getRecipeManager();

            // Get all recipes by type
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new HashMap<>();

            // Group recipes by type
            for (Recipe<?> recipe : recipeManager.getRecipes()) {
                RecipeType<?> type = recipe.getType();
                recipesByType
                        .computeIfAbsent(type, t -> new HashMap<>())
                        .put(recipe.getId(), recipe);
            }

            // Find all smelting and blasting recipes to remove
            List<ResourceLocation> toRemove = new ArrayList<>();

            // Check all registered recipes
            for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : recipesByType.entrySet()) {
                RecipeType<?> type = entry.getKey();

                if (type == RecipeType.SMELTING || type == RecipeType.BLASTING) {
                    for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : entry.getValue().entrySet()) {
                        ResourceLocation id = recipeEntry.getKey();
                        Recipe<?> recipe = recipeEntry.getValue();

                        if (shouldRemoveRecipe(id, recipe, type == RecipeType.SMELTING ? "smelting" : "blasting",
                                metalToIngotMap)) {
                            toRemove.add(id);
                        }
                    }
                }
            }

            // Remove the recipes
            if (!toRemove.isEmpty()) {
                LOGGER.info("Removing {} vanilla smelting/blasting recipes", toRemove.size());

                // Create a new map without the recipes we want to remove
                Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipes = new HashMap<>();

                for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : recipesByType.entrySet()) {
                    Map<ResourceLocation, Recipe<?>> newTypeRecipes = new HashMap<>();

                    for (Map.Entry<ResourceLocation, Recipe<?>> recipeEntry : entry.getValue().entrySet()) {
                        if (!toRemove.contains(recipeEntry.getKey())) {
                            newTypeRecipes.put(recipeEntry.getKey(), recipeEntry.getValue());
                        }
                    }

                    newRecipes.put(entry.getKey(), newTypeRecipes);
                }

                // Replace the recipes map
                try {
                    java.lang.reflect.Field recipesField = RecipeManager.class.getDeclaredField("recipes");
                    recipesField.setAccessible(true);
                    recipesField.set(recipeManager, newRecipes);

                    // Also clear the byName cache
                    java.lang.reflect.Field byNameField = RecipeManager.class.getDeclaredField("byName");
                    byNameField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
                    byName.clear();

                    // Rebuild the byName cache
                    for (Map<ResourceLocation, Recipe<?>> map : newRecipes.values()) {
                        byName.putAll(map);
                    }

                    RECIPES_TO_REMOVE.addAll(toRemove);
                    LOGGER.info("Successfully removed {} recipes", toRemove.size());

                } catch (Exception e) {
                    LOGGER.error("Failed to remove recipes: {}", e.getMessage(), e);
                }
            } else {
                LOGGER.info("No recipes to remove");
            }

        } catch (Exception e) {
            LOGGER.error("Error removing recipes: {}", e.getMessage(), e);
        }
    }

    private static boolean shouldRemoveRecipe(ResourceLocation recipeId, Recipe<?> recipe, String type,
                                              Map<String, String> metalToIngotMap) {
        try {
            // Check if this is a recipe for one of our metals
            String recipePath = recipeId.getPath();
            for (String ingotId : metalToIngotMap.values()) {
                // Extract just the item name (e.g., "gold_ingot" from "minecraft:gold_ingot")
                String itemName = ingotId.contains(":") ? ingotId.split(":")[1] : ingotId;

                // Look for patterns like "gold_ingot_from_smelting" or "gold_ingot_from_blasting"
                // Now matches recipes from any mod namespace
                if (recipePath.startsWith(itemName + "_from_") ||
                        recipePath.endsWith("_to_" + itemName) ||
                        recipePath.equals(itemName + "_smelting") ||
                        recipePath.equals(itemName + "_blasting") ||
                        (recipePath.contains("smelting") && recipePath.contains(itemName)) ||
                        (recipePath.contains("blasting") && recipePath.contains(itemName))) {

                    LOGGER.debug("Matched recipe for removal - Type: {}, Namespace: {}, Path: {}",
                                type, recipeId.getNamespace(), recipePath);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error checking recipe {}: {}", recipeId, e.getMessage(), e);
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onReload(AddReloadListenerEvent event) {
        // Reset initialization state on reload
        initialized = false;
        RECIPES_TO_REMOVE.clear();
    }
}
