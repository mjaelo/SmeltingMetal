package com.smeltingmetal.recipes;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
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

    /**
     * Restores all original recipes that were replaced by this mod.
     * Called when recipes are reloaded to ensure clean state.
     */
    public static void restoreRecipes() {
        if (ORIGINAL_RECIPES.isEmpty()) return;
        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null) return;

        LOGGER.info("Restoring {} vanilla recipes...", ORIGINAL_RECIPES.size());
        ORIGINAL_RECIPES.forEach((id, recipe) -> replaceRecipeInManager(recipeManager, id, recipe));
        ORIGINAL_RECIPES.clear();
        REPLACED_RECIPES.clear();
    }

    /**
     * Replaces vanilla smelting recipes with custom molten metal recipes.
     * Processes all cooking recipes and modifies them to output molten metals.
     */
    public static void replaceRecipes(boolean isReload) {
        LOGGER.info("===== STARTING RECIPE REPLACEMENT =====");

        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null) {
            if (!isReload) {
                LOGGER.warn("RecipeManager is not available, skipping recipe replacement");
            }
            return;
        }

        MinecraftServer server = SmeltingMetalMod.getServer();
        if (server == null) {
            LOGGER.warn("MinecraftServer is not available, skipping recipe replacement");
            return;
        }

        RegistryAccess registryAccess = server.registryAccess();
        Set<RecipeType<?>> recipeTypesToReplace = getTargetRecipeTypes();
        if (recipeTypesToReplace.isEmpty()) {
            LOGGER.warn("No recipe types to replace, check configuration");
            return;
        }

        LOGGER.info("Processing recipe replacement for types: {}", recipeTypesToReplace);
        int totalRecipesProcessed = 0;
        int totalRecipesReplaced = 0;
        try {
            Field recipesField = getRecipesField();
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);

            // remove block crafting recipes first
            removeBlockCraftingRecipes(recipesByType);

            List<Recipe<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());
            LOGGER.info("Found {} total recipes to process", allRecipes.size());

            for (Recipe<?> recipe : allRecipes) {
                totalRecipesProcessed++;

                if (!recipeTypesToReplace.contains(recipe.getType())) {
                    LOGGER.trace("Skipping recipe {} - wrong type: {}", recipe.getId(), recipe.getType());
                    continue;
                }

                if (recipe.getResultItem(registryAccess).isEmpty()) {
                    LOGGER.debug("Skipping recipe {} - empty result", recipe.getId());
                    continue;
                }

                ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(recipe.getResultItem(registryAccess).getItem());
                if (resultId == null) {
                    LOGGER.warn("Skipping recipe {} - could not get item key for result", recipe.getId());
                    continue;
                }

                LOGGER.debug("Processing recipe {} with result {}", recipe.getId(), resultId);
                ResourceLocation finalMoltenIngotId = null;

                // 1) classic detection: ingot/nugget outputs
                ResourceLocation derivedIngot = deriveIngotId(resultId);
                if (derivedIngot != null && ModMetals.getMetalId(derivedIngot) != null) {
                    finalMoltenIngotId = derivedIngot;
                }

                // 2) fallback: detect metal substring in result name (doors, trapdoors, etc.)
                if (finalMoltenIngotId == null) {
                    String metalKey = detectMetalFromResultName(resultId);
                    if (metalKey != null) {
                        finalMoltenIngotId = ForgeRegistries.ITEMS.getKey(ModItems.getMoltenMetalStack(metalKey).getItem());
                    }
                }

                // ---- additional: replace metal-block outputs with molten-metal-block item ----
                if (resultId.getPath().endsWith("_block")) {
                    String base = resultId.getPath().substring(0, resultId.getPath().length() - 6); // iron, gold, etc.
                    String metalKey = base.contains(":") ? base : resultId.getNamespace() + ":" + base;

                    // Confirm known metal or accept anyway
                    if (!ModMetals.doesMetalExist(metalKey)) {
                        LOGGER.debug("Metal {} not registered via ModMetals; still creating molten-block recipe", metalKey);
                    }

                    ItemStack moltenBlock = ModItems.getMoltenMetalBlockStack(metalKey);
                    Recipe<?> newRec = replaceCookingResult(recipe, moltenBlock);
                    if (newRec != null) {
                        ORIGINAL_RECIPES.put(recipe.getId(), recipe);
                        REPLACED_RECIPES.add(recipe.getId());
                        replaceRecipeInManager(recipeManager, recipe.getId(), newRec);
                        LOGGER.debug("Replaced block-smelt recipe {} -> molten block", recipe.getId());
                    }
                }

                // ---- new: if ingredient itself is a metal block/raw block, convert recipe to molten block ----
                ItemStack firstIngStack = recipe.getIngredients().isEmpty() ? ItemStack.EMPTY : recipe.getIngredients().get(0).getItems().length > 0 ? recipe.getIngredients().get(0).getItems()[0] : ItemStack.EMPTY;
                if (!firstIngStack.isEmpty()) {
                    ResourceLocation ingId = ForgeRegistries.ITEMS.getKey(firstIngStack.getItem());
                    if (ingId != null && ingId.getPath().endsWith("_block")) {
                        String base = ingId.getPath().substring(0, ingId.getPath().length() - 6);
                        String metalKey2 = base.contains(":") ? base : ingId.getNamespace() + ":" + base;
                        ItemStack moltenBlock = ModItems.getMoltenMetalBlockStack(metalKey2);
                        Recipe<?> newRec = replaceCookingResult(recipe, moltenBlock);
                        if (newRec != null) {
                            ORIGINAL_RECIPES.put(recipe.getId(), recipe);
                            REPLACED_RECIPES.add(recipe.getId());
                            replaceRecipeInManager(recipeManager, recipe.getId(), newRec);
                            LOGGER.debug("Replaced block-ingredient smelt recipe {} -> molten block", recipe.getId());
                            continue; // skip further processing of this recipe
                        }
                    }
                }

                if (finalMoltenIngotId != null) {
                    // Ensure ingredient isn't a block (nugget allowed)
                    if (recipeInputBlacklisted(recipe)) {
                        LOGGER.debug("Skipping recipe {} - input is blacklisted", recipe.getId());
                        continue;
                    }

                    LOGGER.debug("Creating molten metal recipe for {} with metal ID: {}", recipe.getId(), finalMoltenIngotId);
                    Recipe<?> newRecipe = createMoltenMetalRecipe(recipe, finalMoltenIngotId, registryAccess);
                    if (newRecipe != null) {
                        LOGGER.debug("Successfully created replacement recipe for {}", recipe.getId());
                        try {
                            ORIGINAL_RECIPES.put(recipe.getId(), recipe);
                            REPLACED_RECIPES.add(recipe.getId());
                            boolean replaced = replaceRecipeInManager(recipeManager, recipe.getId(), newRecipe);
                            if (replaced) {
                                LOGGER.info("SUCCESSFULLY REPLACED recipe {} -> {} (original: {})",
                                        recipe.getId(),
                                        ForgeRegistries.ITEMS.getKey(newRecipe.getResultItem(registryAccess).getItem()),
                                        resultId);
                            } else {
                                LOGGER.error("FAILED to replace recipe {} in manager. Original: {}", recipe.getId(), resultId);
                            }
                            totalRecipesReplaced++;
                        } catch (Exception e) {
                            LOGGER.error("Failed to replace recipe {}: {}", recipe.getId(), e.getMessage(), e);
                        }
                    } else {
                        LOGGER.warn("Failed to create replacement recipe for {}", recipe.getId());
                    }
                } else {
                    LOGGER.trace("Skipping recipe {} - no molten metal match found", recipe.getId());
                }
            }
            LOGGER.info("===== RECIPE REPLACEMENT SUMMARY =====");
            LOGGER.info("Total recipes processed: {}", totalRecipesProcessed);
            LOGGER.info("Total recipes replaced: {}", totalRecipesReplaced);
            LOGGER.info("Total recipes in REPLACED_RECIPES: {}", REPLACED_RECIPES.size());
            LOGGER.info("Total recipes in ORIGINAL_RECIPES: {}", ORIGINAL_RECIPES.size());
            LOGGER.info("======================================");

            addMissingMetalItemRecipes(recipeManager, registryAccess, recipeTypesToReplace);
            replaceNuggetCraftingRecipes(recipeManager, registryAccess);
            addRawBlockMoltenRecipes(recipeManager, registryAccess);

            LOGGER.info("===== RECIPE REPLACEMENT COMPLETE =====");
        } catch (Exception e) {
            LOGGER.error("CRITICAL ERROR during recipe replacement: {}", e.getMessage(), e);
            LOGGER.error("Recipes replaced before error: {}", REPLACED_RECIPES.size());
        }
    }

    /**
     * Creates a new cooking recipe that outputs a molten metal item.
     *
     * @param originalRecipe  The original recipe to base the new one on
     * @param originalIngotId The ID of the original ingot being replaced
     * @param registryAccess  Registry access for recipe operations
     * @return A new recipe that outputs molten metal, or null if invalid
     */
    private static Recipe<?> createMoltenMetalRecipe(Recipe<?> originalRecipe, ResourceLocation originalIngotId, RegistryAccess registryAccess) {
        String metalId = ModMetals.getMetalId(originalIngotId);
        if (metalId == null) {
            LOGGER.warn("Could not determine metal ID for item: {}", originalIngotId);
            return null;
        }

        ItemStack resultStack = ModItems.getMoltenMetalStack(metalId);
        if (resultStack.isEmpty()) {
            LOGGER.warn("Failed to create molten metal stack for metal ID: {}", metalId);
            return null;
        }

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

    /**
     * Creates a new cooking recipe with a different output item.
     * Preserves all other properties from the original recipe.
     *
     * @param originalRecipe The recipe to copy properties from
     * @param newResult      The new output item stack
     * @return A new recipe with the updated output, or null if type is unsupported
     */
    private static Recipe<?> replaceCookingResult(Recipe<?> originalRecipe, ItemStack newResult) {
        if (!(originalRecipe instanceof AbstractCookingRecipe acr)) return null;
        Ingredient ing = acr.getIngredients().get(0);
        float xp = acr.getExperience();
        int time = acr.getCookingTime();
        String group = acr.getGroup();
        CookingBookCategory cat = acr.category();
        ResourceLocation id = originalRecipe.getId();
        if (originalRecipe.getType() == RecipeType.SMELTING) {
            return new SmeltingRecipe(id, group, cat, ing, newResult, xp, time);
        } else if (originalRecipe.getType() == RecipeType.BLASTING) {
            return new BlastingRecipe(id, group, cat, ing, newResult, xp, time);
        } else if (originalRecipe.getType() == RecipeType.CAMPFIRE_COOKING) {
            return new CampfireCookingRecipe(id, group, cat, ing, newResult, xp, time);
        } else if (originalRecipe.getType() == RecipeType.SMOKING) {
            return new SmokingRecipe(id, group, cat, ing, newResult, xp, time);
        }
        return null;
    }

    /**
     * Replaces a recipe in the recipe manager using reflection.
     * @param recipeManager The recipe manager to modify
     * @param recipeId The ID of the recipe to replace
     * @param newRecipe The new recipe to insert
     */
    /**
     * Replaces a recipe in the recipe manager.
     *
     * @param recipeManager The recipe manager to modify
     * @param recipeId      The ID of the recipe to replace
     * @param newRecipe     The new recipe to insert
     * @return true if replacement was successful, false otherwise
     */
    private static boolean replaceRecipeInManager(RecipeManager recipeManager, ResourceLocation recipeId, Recipe<?> newRecipe) {
        try {
            LOGGER.debug("Attempting to replace recipe: {}", recipeId);
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
            LOGGER.debug("Successfully replaced recipe in manager: {}", recipeId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to update recipe manager for recipe: {}", recipeId, e);
            LOGGER.error("Error details - Recipe type: {}, Result item: {}",
                    newRecipe.getType(),
                    newRecipe.getResultItem(SmeltingMetalMod.getServer().registryAccess()).getItem());
            return false;
        }
    }

    /**
     * Gets the private recipes field from RecipeManager using reflection.
     *
     * @return The accessible recipes field
     * @throws NoSuchFieldException If the field cannot be found
     */
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

    /**
     * Gets the set of recipe types that should be processed for replacement.
     *
     * @return Set of recipe types to process (smelting, blasting, etc.)
     */
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

    /**
     * Derives an ingot ID from a result ID.
     *
     * @param resultId The result item ID to process
     * @return The corresponding ingot ID, or null if not applicable
     */
    private static ResourceLocation deriveIngotId(ResourceLocation resultId) {
        if (resultId == null) return null;
        String path = resultId.getPath();
        if (path.endsWith("_ingot")) return resultId;
        if (path.endsWith("_nugget")) {
            String base = path.substring(0, path.length() - 7); // remove _nugget
            return new ResourceLocation(resultId.getNamespace(), base + "_ingot");
        }
        return null;
    }

    /**
     * Detects metal type from a result item's name.
     *
     * @param resultId The result item ID to analyze
     * @return The metal key if detected, null otherwise
     */
    private static String detectMetalFromResultName(ResourceLocation resultId) {
        if (resultId == null) return null;
        String path = resultId.getPath().toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (path.contains(bad)) return null;
        }
        for (String metalKey : ModMetals.getAllMetalProperties().keySet()) {
            String keyName = metalKey.contains(":") ? metalKey.split(":", 2)[1] : metalKey;
            if (path.contains(keyName.toLowerCase())) return metalKey;
        }
        return null;
    }

    /**
     * Checks if a recipe's input items match any blacklisted keywords.
     *
     * @param recipe The recipe to check
     * @return true if any input is blacklisted, false otherwise
     */
    private static boolean recipeInputBlacklisted(Recipe<?> recipe) {
        if (!(recipe instanceof AbstractCookingRecipe cooking)) {
            LOGGER.trace("Recipe {} is not a cooking recipe, not blacklisted", recipe.getId());
            return false;
        }
        if (cooking.getIngredients().isEmpty()) {
            LOGGER.debug("Recipe {} has no ingredients, not blacklisted", recipe.getId());
            return false;
        }
        Ingredient ing = cooking.getIngredients().get(0);
        for (ItemStack st : ing.getItems()) {
            ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
            if (rid == null) continue;
            String p = rid.getPath().toLowerCase();
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (p.contains(bad)) {
                    LOGGER.debug("Recipe {} is blacklisted due to keyword '{}' in path: {}", recipe.getId(), bad, p);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds missing molten metal recipes for metal items that don't have them.
     *
     * @param recipeManager  The recipe manager to add recipes to
     * @param registryAccess Registry access for recipe operations
     * @param targetTypes    The recipe types to create recipes for
     */
    private static void addMissingMetalItemRecipes(RecipeManager recipeManager, RegistryAccess registryAccess, Set<RecipeType<?>> targetTypes) {
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;
            String path = itemId.getPath().toLowerCase();
            boolean skip = false;
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (path.contains(bad)) {
                    skip = true;
                    break;
                }
            }
            if (skip || path.contains("ingot") || path.contains("raw_")) continue;

            String matchedMetal = null;
            for (String metalKey : ModMetals.getAllMetalProperties().keySet()) {
                String keyName = metalKey.contains(":") ? metalKey.split(":", 2)[1] : metalKey;
                if (path.contains(keyName.toLowerCase())) {
                    matchedMetal = metalKey;
                    break;
                }
            }
            if (matchedMetal == null) continue;

            // Check if there is already a cooking recipe for this item in any target type
            boolean hasCook = false;
            for (Recipe<?> r : recipeManager.getRecipes()) {
                if (r instanceof AbstractCookingRecipe acr && !acr.getIngredients().isEmpty() && acr.getIngredients().get(0).test(new ItemStack(item))) {
                    hasCook = true;
                    break;
                }
            }
            if (hasCook) continue;

            ItemStack resultStack = ModItems.getMoltenMetalStack(matchedMetal);
            if (resultStack.isEmpty()) continue;

            Ingredient ing = Ingredient.of(item);
            for (RecipeType<?> tp : targetTypes) {
                int cookTime = tp == RecipeType.BLASTING ? 100 : 200;
                float xp = 0.1f;
                CookingBookCategory cat = CookingBookCategory.MISC;
                String suffix = tp == RecipeType.BLASTING ? "_blasting" : "_smelting";
                ResourceLocation newId = new ResourceLocation(SmeltingMetalMod.MODID,
                        "auto_" + itemId.getNamespace() + "_" + itemId.getPath() + suffix);

                AbstractCookingRecipe newRec = tp == RecipeType.BLASTING ?
                        new BlastingRecipe(newId, "", cat, ing, resultStack, xp, cookTime) :
                        new SmeltingRecipe(newId, "", cat, ing, resultStack, xp, cookTime);

                replaceRecipeInManager(recipeManager, newId, newRec);
                LOGGER.debug("Added auto molten recipe {} for {}", newId, itemId);
            }
        }
    }

    /**
     * Adds recipes for smelting raw metal blocks into molten metal blocks.
     * Handles both vanilla-style raw items and direct raw blocks.
     *
     * @param recipeManager  The recipe manager to add recipes to
     * @param registryAccess Registry access for recipe operations
     */
    private static void addRawBlockMoltenRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        LOGGER.info("===== ADDING RAW BLOCK MOLTEN RECIPES =====");
        int addedRecipes = 0;

        // First, find all raw metal blocks and create recipes for them
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;

            String path = itemId.getPath();
            if (!path.startsWith("raw_") || !path.endsWith("_block")) continue;

            // Extract metal name (e.g., "iron" from "raw_iron_block")
            String metalKey = path.substring(4, path.length() - 6); // strip "raw_" and "_block"
            LOGGER.debug("Found raw metal block: {} for metal: {}", itemId, metalKey);

            // Get the corresponding molten metal block
            ItemStack moltenBlock = ModItems.getMoltenMetalBlockStack(itemId.getNamespace() + ":" + metalKey);
            if (moltenBlock.isEmpty()) {
                LOGGER.warn("No molten metal block found for metal: {}", metalKey);
                continue;
            }

            // Create both smelting and blasting recipes
            createAndAddRawBlockRecipe(recipeManager, item, moltenBlock, "smelting", 200, 0.7f, addedRecipes);
            createAndAddRawBlockRecipe(recipeManager, item, moltenBlock, "blasting", 100, 0.7f, addedRecipes);

            addedRecipes += 2;
        }

        LOGGER.info("Added {} raw block molten recipes", addedRecipes);
        LOGGER.info("======================================");
    }

    private static void createAndAddRawBlockRecipe(RecipeManager recipeManager, Item input, ItemStack output,
                                                   String type, int baseTime, float xp, int index) {
        String metalKey = ForgeRegistries.ITEMS.getKey(input).getPath()
                .replace("raw_", "")
                .replace("_block", "");

        ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID,
                String.format("%s_raw_%s_block_%d", type, metalKey, index));

        Ingredient ingredient = Ingredient.of(input);

        Recipe<?> recipe;
        if ("smelting".equals(type)) {
            recipe = new SmeltingRecipe(recipeId, "", CookingBookCategory.MISC,
                    ingredient, output, xp, baseTime);
        } else {
            recipe = new BlastingRecipe(recipeId, "", CookingBookCategory.MISC,
                    ingredient, output, xp, baseTime / 2);
        }

        boolean success = replaceRecipeInManager(recipeManager, recipeId, recipe);
        if (success) {
            LOGGER.info("Added {} recipe for {} -> {}", type,
                    ForgeRegistries.ITEMS.getKey(input),
                    ForgeRegistries.ITEMS.getKey(output.getItem()));
        } else {
            LOGGER.warn("Failed to add {} recipe for {}", type,
                    ForgeRegistries.ITEMS.getKey(input));
        }
    }

    /**
     * Removes vanilla block crafting recipes to prevent duplication with smelting.
     *
     * @param recipesByType Map of recipe types to their recipes
     */
    private static void removeBlockCraftingRecipes(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType) {
        Map<ResourceLocation, Recipe<?>> crafting = recipesByType.get(RecipeType.CRAFTING);
        if (crafting == null) return;
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Recipe<?> r : crafting.values()) {
            if (!(r instanceof ShapedRecipe shaped)) continue;
            // 3x3 grid
            if (shaped.getWidth() != 3 || shaped.getHeight() != 3) continue;
            ItemStack result = shaped.getResultItem(RegistryAccess.EMPTY);
            if (result.isEmpty()) continue;
            ResourceLocation rid = ForgeRegistries.ITEMS.getKey(result.getItem());
            if (rid == null) continue;
            String metalId = ModMetals.getMetalId(rid);
            if (metalId == null) {
                // For vanilla blocks like iron_block the item id path ends with _block; derive metal
                String p = rid.getPath();
                if (p.endsWith("_block")) {
                    String candidate = p.substring(0, p.length() - 6);
                    if (ModMetals.doesMetalExist(candidate)) metalId = candidate;
                }
            }
            if (metalId == null) continue;
            boolean allIngots = shaped.getIngredients().stream().allMatch(ing -> {
                if (ing.isEmpty()) return false;
                ItemStack any = ing.getItems().length > 0 ? ing.getItems()[0] : ItemStack.EMPTY;
                ResourceLocation iid = ForgeRegistries.ITEMS.getKey(any.getItem());
                return iid != null && iid.getPath().endsWith("ingot");
            });
            if (!allIngots) continue;
            toRemove.add(r.getId());
        }
        toRemove.forEach(crafting::remove);
        REPLACED_RECIPES.addAll(toRemove);
    }

    /**
     * Handles server start event to initialize recipe replacement.
     *
     * @param event The server started event
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Ensure static reference is ready
        SmeltingMetalMod.setRecipeManager(event.getServer().getRecipeManager());
        SmeltingMetalMod.setServer(event.getServer());

        LOGGER.info("Server started, triggering initial recipe replacement.");
        replaceRecipes(false);
    }

    /**
     * Handles recipe reload events to ensure proper recipe replacement.
     *
     * @param event The reload event
     */
    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        restoreRecipes();
        replaceRecipes(true);
    }
}
