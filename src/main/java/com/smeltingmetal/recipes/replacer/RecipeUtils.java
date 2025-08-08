package com.smeltingmetal.recipes.replacer;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.smeltingmetal.MetalsConfig;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.ModMetals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

public class RecipeUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void replaceRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId, Recipe<?> newRecipe) {
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

    public static String getMetalKeyFromItem(ResourceLocation itemId) {
        if (itemId == null) return null;
        String path = itemId.getPath();

        ResourceLocation derivedIngotId = deriveIngotId(itemId);
        if (derivedIngotId != null) {
            String metalId = ModMetals.getMetalId(derivedIngotId);
            if (metalId != null) return metalId;
        }

        String pathLower = path.toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (pathLower.contains(bad)) return null;
        }
        for (String metalKey : ModMetals.getAllMetalProperties().keySet()) {
            String keyName = metalKey.contains(":") ? metalKey.split(":")[1] : metalKey;
            if (pathLower.contains(keyName.toLowerCase())) return metalKey;
        }

        return null;
    }

    private static ResourceLocation deriveIngotId(ResourceLocation resultId) {
        if (resultId == null) return null;
        String path = resultId.getPath();
        if (path.endsWith("_ingot")) return resultId;
        if (path.endsWith("_nugget")) {
            String base = path.substring(0, path.length() - 7);
            return new ResourceLocation(resultId.getNamespace(), base + "_ingot");
        }
        return null;
    }

    public static boolean isInputBlacklisted(Recipe<?> recipe) {
        if (!(recipe instanceof AbstractCookingRecipe cooking) || cooking.getIngredients().isEmpty()) return false;
        Ingredient ing = cooking.getIngredients().get(0);
        for (ItemStack st : ing.getItems()) {
            ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
            if (rid == null) continue;
            String p = rid.getPath().toLowerCase();
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (p.contains(bad)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isInputBlacklisted(ItemStack stack) {
        ResourceLocation rid = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rid == null) return false;
        String p = rid.getPath().toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (p.contains(bad)) {
                return true;
            }
        }
        return false;
    }



    public static boolean removeRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId) {
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            // Get the recipe to find its type before removing
            // Use byKey to get the actual recipe object, which contains the type
            Recipe<?> recipeToRemove = recipeManager.byKey(recipeId).orElse(null);
            if (recipeToRemove == null) {
                LOGGER.warn("Attempted to remove recipe '{}' but it was not found in the manager.", recipeId);
                return false;
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

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to remove recipe: {}", recipeId, e);
            return false;
        }
    }

    public static void createAndAddCookingRecipe(RecipeManager recipeManager, Item input, ItemStack output, RecipeType<?> type, int time, float xp) {
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(input);
        if (inputId == null) return;

        String typeName = type == RecipeType.SMELTING ? "smelting" : "blasting";
        String idName = String.format("%s_%s_%s", typeName, inputId.getNamespace(), inputId.getPath());

        ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, idName);
        Ingredient ingredient = Ingredient.of(input);

        AbstractCookingRecipe recipe = type == RecipeType.SMELTING ?
                new SmeltingRecipe(recipeId, "", CookingBookCategory.MISC, ingredient, output, xp, time) :
                new BlastingRecipe(recipeId, "", CookingBookCategory.MISC, ingredient, output, xp, time / 2);

        replaceRecipeInManager(recipeManager, recipeId, recipe);
    }

    public static void createAndAddCrushingRecipe(RecipeManager recipeManager, Item input, ItemStack result) {
        try {
            // Create a unique recipe ID based on the input and output items
            ResourceLocation recipeId = new ResourceLocation(
                SmeltingMetalMod.MODID,
                "crushing/" + ForgeRegistries.ITEMS.getKey(input).getPath() + "_to_" + ForgeRegistries.ITEMS.getKey(result.getItem()).getPath()
            );
            
            // Create a new processing recipe builder for crushing
            ProcessingRecipeBuilder<CrushingRecipe> builder = new ProcessingRecipeBuilder<>(
                CrushingRecipe::new,  // Recipe factory
                recipeId             // Recipe ID
            );
            
            // Configure and build the recipe
            builder.withItemIngredients(Ingredient.of(input))  // Input ingredient
                   .output(result)                             // Output result
                   .output(.1f, result)                        // Secondary output with 10% chance
                   .build();
            
            // Manually register the recipe since we're not in a datagen context
            CrushingRecipe recipe = builder.build();
            replaceRecipeInManager(recipeManager, recipeId, recipe);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create crushing recipe for {} -> {}: {}", 
                ForgeRegistries.ITEMS.getKey(input), 
                ForgeRegistries.ITEMS.getKey(result.getItem()),
                e.getMessage());
        }
    }
}