package com.smeltingmetal.recipes;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID)
public class RecipeRemoval {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, Recipe<?>> ORIGINAL_RECIPES = new HashMap<>();
    private static final Set<ResourceLocation> REPLACED_RECIPES = new HashSet<>();

    public static void restoreRecipes() {
        if (ORIGINAL_RECIPES.isEmpty()) return;
        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null) return;

        LOGGER.info("Restoring {} vanilla recipes...", ORIGINAL_RECIPES.size());
        ORIGINAL_RECIPES.forEach((id, recipe) -> replaceRecipeInManager(recipeManager, id, recipe));
        ORIGINAL_RECIPES.clear();
        REPLACED_RECIPES.clear();
    }

    public static void replaceRecipes() {
        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null) return;
        MinecraftServer server = SmeltingMetalMod.getServer();
        if (server == null) return;

        RegistryAccess registryAccess = server.registryAccess();
        Set<RecipeType<?>> recipeTypesToReplace = getTargetRecipeTypes();
        if (recipeTypesToReplace.isEmpty()) return;

        LOGGER.info("Starting recipe replacement for types: {}", recipeTypesToReplace);
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : recipesByType.entrySet()) {
                if (!recipeTypesToReplace.contains(entry.getKey())) continue;

                List<Pair<Recipe<?>, ResourceLocation>> recipesToReplaceList = new ArrayList<>();
                for (Recipe<?> recipe : entry.getValue().values()) {
                    ItemStack result = recipe.getResultItem(registryAccess);
                    if (!result.isEmpty()) {
                        ResourceLocation ingotId = ForgeRegistries.ITEMS.getKey(result.getItem());
                        if (ModMetals.getMetalId(ingotId) != null) {
                            recipesToReplaceList.add(Pair.of(recipe, ingotId));
                        }
                    }
                }

                for (Pair<Recipe<?>, ResourceLocation> pair : recipesToReplaceList) {
                    Recipe<?> originalRecipe = pair.getFirst();
                    if (REPLACED_RECIPES.contains(originalRecipe.getId())) continue;

                    Recipe<?> newRecipe = createMoltenMetalRecipe(originalRecipe, pair.getSecond(), registryAccess);
                    if (newRecipe != null) {
                        ORIGINAL_RECIPES.put(originalRecipe.getId(), originalRecipe);
                        REPLACED_RECIPES.add(originalRecipe.getId());
                        replaceRecipeInManager(recipeManager, originalRecipe.getId(), newRecipe);
                        LOGGER.debug("Replaced recipe {} with molten metal variant for {}", originalRecipe.getId(), pair.getSecond());
                    }
                }
            }
            LOGGER.info("Finished recipe replacement. Replaced {} recipes.", REPLACED_RECIPES.size());
            replaceNuggetCraftingRecipes(recipeManager, registryAccess);
        } catch (Exception e) {
            LOGGER.error("Failed to access recipe manager fields via reflection", e);
        }
    }

    private static Recipe<?> createMoltenMetalRecipe(Recipe<?> originalRecipe, ResourceLocation originalIngotId, RegistryAccess registryAccess) {
        String metalId = ModMetals.getMetalId(originalIngotId);
        if (metalId == null) return null;

        ItemStack resultStack = ModItems.getMoltenMetalStack(metalId);
        if (resultStack.isEmpty()) return null;

        if (originalRecipe instanceof AbstractCookingRecipe originalCookingRecipe) {
            Ingredient ingredient = originalCookingRecipe.getIngredients().get(0);
            float experience = originalCookingRecipe.getExperience();
            int cookingTime = originalCookingRecipe.getCookingTime();
            String group = originalCookingRecipe.getGroup();
            CookingBookCategory category = originalCookingRecipe.category();
            ResourceLocation id = originalRecipe.getId();

            if (originalRecipe.getType() == RecipeType.SMELTING) {
                return new SmeltingRecipe(id, group, category, ingredient, resultStack, experience, cookingTime);
            } else if (originalRecipe.getType() == RecipeType.BLASTING) {
                return new BlastingRecipe(id, group, category, ingredient, resultStack, experience, cookingTime);
            } else if (originalRecipe.getType() == RecipeType.SMOKING) {
                return new SmokingRecipe(id, group, category, ingredient, resultStack, experience, cookingTime);
            } else if (originalRecipe.getType() == RecipeType.CAMPFIRE_COOKING) {
                return new CampfireCookingRecipe(id, group, category, ingredient, resultStack, experience, cookingTime);
            }
        }
        return null;
    }

    private static void replaceRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId, Recipe<?> newRecipe) {
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newRecipesByType = new HashMap<>(recipesByType);
            Map<ResourceLocation, Recipe<?>> innerMap = new HashMap<>(newRecipesByType.get(newRecipe.getType()));
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
            LOGGER.error("Failed to update recipe manager for recipe: " + recipeId, e);
        }
    }

    private static Field getRecipesField() throws NoSuchFieldException {
        // Try known names first
        try {
            return RecipeManager.class.getDeclaredField("recipes");
        } catch (NoSuchFieldException ignored) {
        }
        // Fallback: look for first Map field matching the expected type
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("Could not find recipes field in RecipeManager (obfuscation mismatch)");
    }

    private static Field getByNameField() throws NoSuchFieldException {
        try {
            return RecipeManager.class.getDeclaredField("byName");
        } catch (NoSuchFieldException ignored) {
        }
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                // skip the first map which is recipes
                continue;
            }
        }
        // As a fallback, iterate again and return any other map field (second one)
        int count = 0;
        for (Field field : RecipeManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                count++;
                if (count == 2) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new NoSuchFieldException("Could not find byName field in RecipeManager (obfuscation mismatch)");
    }

    private static Set<RecipeType<?>> getTargetRecipeTypes() {
        Set<RecipeType<?>> recipeTypes = new HashSet<>();
        if (MetalsConfig.CONFIG == null || MetalsConfig.CONFIG.recipeTypes == null) return recipeTypes;
        for (String typeName : MetalsConfig.CONFIG.recipeTypes.get()) {
            Optional.ofNullable(ForgeRegistries.RECIPE_TYPES.getValue(new ResourceLocation(typeName))).ifPresent(recipeTypes::add);
        }
        return recipeTypes;
    }

    private static void replaceNuggetCraftingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            if (recipe.getType() != RecipeType.CRAFTING) continue;
            if (!(recipe instanceof ShapedRecipe shaped)) continue;
            // Only consider full 3x3 grid
            if (shaped.getWidth() != 3 || shaped.getHeight() != 3) continue;

            ItemStack result = shaped.getResultItem(registryAccess);
            if (result.isEmpty() || result.getCount() != 1) continue;

            ResourceLocation ingotId = ForgeRegistries.ITEMS.getKey(result.getItem());
            String metalId = ModMetals.getMetalId(ingotId);
            if (metalId == null) continue; // not one of our tracked metals

            // derive nugget and raw ore ids
            String base = ingotId.getPath().replace("_ingot", "").replace("ingot", "");
            if (base.startsWith("raw_")) base = base.substring(4);
            ResourceLocation nuggetId = new ResourceLocation(ingotId.getNamespace(), base + "_nugget");
            ResourceLocation rawOreId = new ResourceLocation(ingotId.getNamespace(), "raw_" + base);

            // ensure raw ore item exists
            if (!ForgeRegistries.ITEMS.containsKey(rawOreId)) continue;

            ItemStack nuggetStack = new ItemStack(ForgeRegistries.ITEMS.getValue(nuggetId));
            boolean allNuggets = shaped.getIngredients().stream().allMatch(ing -> !ing.isEmpty() && ing.test(nuggetStack));
            if (!allNuggets) continue;

            // create new recipe result
            ItemStack newResult = new ItemStack(ForgeRegistries.ITEMS.getValue(rawOreId));
            ShapedRecipe newRecipe = new ShapedRecipe(
                    recipe.getId(),
                    shaped.getGroup(),
                    shaped.category(),
                    shaped.getWidth(), shaped.getHeight(),
                    shaped.getIngredients(),
                    newResult);

            ORIGINAL_RECIPES.put(recipe.getId(), recipe);
            replaceRecipeInManager(recipeManager, recipe.getId(), newRecipe);
            LOGGER.debug("Replaced nugget crafting recipe {} with raw ore output", recipe.getId());
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, triggering initial recipe replacement.");
        replaceRecipes();
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        restoreRecipes();
        replaceRecipes();
    }
}
