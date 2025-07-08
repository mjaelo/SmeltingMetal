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

            // remove block crafting recipes first
            removeBlockCraftingRecipes(recipesByType);

            for (Recipe<?> recipe : recipeManager.getRecipes()) {
                if (!recipeTypesToReplace.contains(recipe.getType())) continue;

                if (recipe.getResultItem(registryAccess).isEmpty()) continue;
                ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(recipe.getResultItem(registryAccess).getItem());
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
                    if (recipeInputBlacklisted(recipe)) continue;
                    Recipe<?> newRecipe = createMoltenMetalRecipe(recipe, finalMoltenIngotId, registryAccess);
                    if (newRecipe != null) {
                        ORIGINAL_RECIPES.put(recipe.getId(), recipe);
                        REPLACED_RECIPES.add(recipe.getId());
                        replaceRecipeInManager(recipeManager, recipe.getId(), newRecipe);
                        LOGGER.debug("Replaced recipe {} with molten metal variant for {}", recipe.getId(), finalMoltenIngotId);
                    }
                }
            }
            LOGGER.info("Finished recipe replacement. Replaced {} recipes.", REPLACED_RECIPES.size());
            addMissingMetalItemRecipes(recipeManager, registryAccess, recipeTypesToReplace);
            replaceNuggetCraftingRecipes(recipeManager, registryAccess);
            addRawBlockMoltenRecipes(recipeManager, registryAccess);
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

    /**
     * If result is ingot returns same id, if nugget returns matching ingot id, otherwise null.
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

    private static String detectMetalFromResultName(ResourceLocation resultId) {
        if (resultId == null) return null;
        String path = resultId.getPath().toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (path.contains(bad)) return null;
        }
        for (String metalKey : ModMetals.getAllMetalProperties().keySet()) {
            String keyName = metalKey.contains(":") ? metalKey.split(":",2)[1] : metalKey;
            if (path.contains(keyName.toLowerCase())) return metalKey;
        }
        return null;
    }

    private static boolean recipeInputBlacklisted(Recipe<?> recipe) {
        if (!(recipe instanceof AbstractCookingRecipe cooking)) return false;
        if (cooking.getIngredients().isEmpty()) return false;
        Ingredient ing = cooking.getIngredients().get(0);
        for (ItemStack st : ing.getItems()) {
            ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
            if (rid == null) continue;
            String p = rid.getPath().toLowerCase();
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (p.contains(bad)) return true;
            }
        }
        return false;
    }

    private static void addMissingMetalItemRecipes(RecipeManager recipeManager, RegistryAccess registryAccess, Set<RecipeType<?>> targetTypes) {
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) continue;
            String path = itemId.getPath().toLowerCase();
            boolean skip = false;
            for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
                if (path.contains(bad)) { skip=true; break; }
            }
            if (skip || path.contains("ingot") || path.contains("raw_")) continue;

            String matchedMetal = null;
            for (String metalKey : ModMetals.getAllMetalProperties().keySet()) {
                String keyName = metalKey.contains(":") ? metalKey.split(":",2)[1] : metalKey;
                if (path.contains(keyName.toLowerCase())) { matchedMetal = metalKey; break; }
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

    private static void addRawBlockMoltenRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            if (!(recipe instanceof AbstractCookingRecipe acr)) continue;
            // look for raw_<metal> item recipe
            Ingredient ing = acr.getIngredients().get(0);
            ItemStack[] stacks = ing.getItems();
            if (stacks.length == 0) continue;
            ResourceLocation firstId = ForgeRegistries.ITEMS.getKey(stacks[0].getItem());
            if (firstId == null) continue;
            if (!firstId.getPath().startsWith("raw_") || firstId.getPath().endsWith("_block")) continue;

            String base = firstId.getPath().substring(4); // strip raw_
            String metalKey = firstId.getNamespace() + ":" + base;

            // derive raw block id
            ResourceLocation rawBlockId = new ResourceLocation(firstId.getNamespace(), "raw_" + base + "_block");
            if (!ForgeRegistries.ITEMS.containsKey(rawBlockId)) continue;

            // if recipe already exists for raw block skip
            Item rawBlockItem = ForgeRegistries.ITEMS.getValue(rawBlockId);
            boolean exists = false;
            for (Recipe<?> r : recipeManager.getRecipes()) {
                if (r instanceof AbstractCookingRecipe c && !c.getIngredients().isEmpty() && c.getIngredients().get(0).test(new ItemStack(rawBlockItem))) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;

            // Build new recipe mirroring the original type/time/xp *9
            int time = acr.getCookingTime() * 2; // raw block cooks slower but not x9
            float xp = acr.getExperience() * 2f;
            CookingBookCategory cat = acr.category();
            String group = acr.getGroup();
            Ingredient newIng = Ingredient.of(rawBlockItem);
            ItemStack result = ModItems.getMoltenMetalBlockStack(metalKey);

            ResourceLocation newId = new ResourceLocation(SmeltingMetalMod.MODID,
                    "smelt_raw_block_" + metalKey.replace(':', '_'));

            Recipe<?> newRecipe;
            if (recipe.getType() == RecipeType.SMELTING) {
                newRecipe = new SmeltingRecipe(newId, group, cat, newIng, result, xp, time);
            } else if (recipe.getType() == RecipeType.BLASTING) {
                newRecipe = new BlastingRecipe(newId, group, cat, newIng, result, xp, time);
            } else {
                continue; // skip unsupported cooking type
            }

            replaceRecipeInManager(recipeManager, newId, newRecipe);
            LOGGER.debug("Added new raw block -> molten block recipe {}", newId);
        }
    }

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
                    String candidate = p.substring(0, p.length()-6);
                    if (ModMetals.doesMetalExist(candidate)) metalId = candidate;
                }
            }
            if (metalId == null) continue;
            boolean allIngots = shaped.getIngredients().stream().allMatch(ing -> {
                if (ing.isEmpty()) return false;
                ItemStack any = ing.getItems().length>0 ? ing.getItems()[0] : ItemStack.EMPTY;
                ResourceLocation iid = ForgeRegistries.ITEMS.getKey(any.getItem());
                return iid != null && iid.getPath().endsWith("ingot");
            });
            if (!allIngots) continue;
            toRemove.add(r.getId());
        }
        toRemove.forEach(crafting::remove);
        REPLACED_RECIPES.addAll(toRemove);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Ensure static reference is ready
        SmeltingMetalMod.setRecipeManager(event.getServer().getRecipeManager());
        SmeltingMetalMod.setServer(event.getServer());

        LOGGER.info("Server started, triggering initial recipe replacement.");
        replaceRecipes();
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        restoreRecipes();
        replaceRecipes();
    }
}
