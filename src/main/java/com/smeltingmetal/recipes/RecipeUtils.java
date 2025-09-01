package com.smeltingmetal.recipes;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.config.MetalsConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RecipeUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void createInRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId, Recipe<?> newRecipe) {
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipesByType = new HashMap<>(recipesByType);
            Map<ResourceLocation, Recipe<?>> innerMap = new HashMap<>(newRecipesByType.getOrDefault(newRecipe.getType(), new HashMap<>()));
            innerMap.put(recipeId, newRecipe);
            newRecipesByType.put(newRecipe.getType(), Collections.unmodifiableMap(innerMap));

            recipesField.set(recipeManager, Collections.unmodifiableMap(newRecipesByType));

            Field byNameField = getByNameField();
            byNameField.setAccessible(true);
            Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
            Map<ResourceLocation, Recipe<?>> newByName = new HashMap<>(byName);
            newByName.put(recipeId, newRecipe);
            byNameField.set(recipeManager, Collections.unmodifiableMap(newByName));
        } catch (Exception e) {
            LOGGER.error("Failed to update recipe manager for recipe: {}", recipeId, e);
        }
    }

    private static Field getRecipesField() throws NoSuchFieldException {
        try {
            return RecipeManager.class.getDeclaredField("recipes");
        } catch (NoSuchFieldException ignored) {}
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("Could not find 'recipes' field in RecipeManager.");
    }

    private static Field getByNameField() throws NoSuchFieldException {
        try {
            return RecipeManager.class.getDeclaredField("byName");
        } catch (NoSuchFieldException ignored) {}
        int mapCount = 0;
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                mapCount++;
                if (mapCount == 2) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new NoSuchFieldException("Could not find 'byName' field in RecipeManager.");
    }

    public static boolean isInputNotBlacklisted(Recipe<?> recipe) {
        if (!(recipe instanceof AbstractCookingRecipe cooking) || cooking.getIngredients().isEmpty()) return true;
        Ingredient ing = cooking.getIngredients().get(0);
        for (ItemStack st : ing.getItems()) {
            ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
            if (rid == null) continue;
            String p = rid.getPath().toLowerCase();
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (p.contains(bad)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static @Nullable ResourceLocation getRecipeResultLocation(RegistryAccess registryAccess, Recipe<?> recipe) {
        ItemStack resultStack = recipe.getResultItem(registryAccess);
        if (resultStack.isEmpty()) {
            return null;
        }

        ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(resultStack.getItem());
        return resultId;
    }

    public static boolean isItemNotBlacklisted(ResourceLocation rid) {
        if (rid == null) return true;
        String p = rid.getPath().toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (p.contains(bad)) {
                return false;
            }
        }
        return true;
    }

    public static void removeRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId) {
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            // Get the recipe to find its type before removing
            // Use byKey to get the actual recipe object, which contains the type
            Recipe<?> recipeToRemove = recipeManager.byKey(recipeId).orElse(null);
            if (recipeToRemove == null) {
                LOGGER.warn("Attempted to remove recipe '{}' but it was not found in the manager.", recipeId);
                return ;
            }
            RecipeType<?> type = recipeToRemove.getType();

            // Create mutable copies to modify
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipesByType = new HashMap<>(recipesByType);
            // Ensure the inner map exists before attempting to remove
            Map<ResourceLocation, Recipe<?>> innerMap = new HashMap<>(newRecipesByType.getOrDefault(type, new HashMap<>()));
            innerMap.remove(recipeId);
            newRecipesByType.put(type, Collections.unmodifiableMap(innerMap));

            // Set the new, unmodifiable map back to the field
            recipesField.set(recipeManager, Collections.unmodifiableMap(newRecipesByType));

            // Update the 'byName' map as well
            Field byNameField = getByNameField();
            byNameField.setAccessible(true);
            Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
            Map<ResourceLocation, Recipe<?>> newByName = new HashMap<>(byName);
            newByName.remove(recipeId);
            byNameField.set(recipeManager, Collections.unmodifiableMap(newByName));

        } catch (Exception e) {
            LOGGER.error("Failed to remove recipe: {}", recipeId, e);
        }
    }
}