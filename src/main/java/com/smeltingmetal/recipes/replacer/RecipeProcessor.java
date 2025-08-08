package com.smeltingmetal.recipes.replacer;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.MetalsConfig;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.smeltingmetal.SmeltingMetalMod.MODID;

/**
 * This class is used to process and modify recipes.
 */
public class RecipeProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void process(RecipeManager recipeManager, RegistryAccess registryAccess) {
        if (recipeManager == null) {
            LOGGER.warn(MODID + " RecipeManager is not available, skipping recipe modifications.");
            return;
        }

        if (MetalsConfig.CONFIG.enableMeltingRecipeReplacement.get()) {
            removeExistingMetalMeltingRecipes(recipeManager, registryAccess);
            removeBlockCraftingRecipes(recipeManager);
            addNewMetalMeltingRecipes(recipeManager);
        }

        boolean isCreateLoaded = net.minecraftforge.fml.ModList.get().isLoaded("create");
        if (isCreateLoaded && MetalsConfig.CONFIG.enableCrushingRecipeReplacement.get()) {
            removeExistingCrushingRecipes(recipeManager, registryAccess);
            addNewCrushingRecipes(recipeManager);
        }

        // handle special recipes
        if (MetalsConfig.CONFIG.enableNuggetRecipeReplacement.get()) {
            replaceNuggetCraftingRecipes(recipeManager, registryAccess);
            addReverseNuggetCraftingRecipes(recipeManager);
        }

        syncRecipesToPlayers();
        LOGGER.info("===== RECIPE MODIFICATIONS COMPLETE =====");
    }

    private static void syncRecipesToPlayers() {
        if (SmeltingMetalMod.getServer() == null) return;
        var packet = new ClientboundUpdateRecipesPacket(
                SmeltingMetalMod.getServer().getRecipeManager().getRecipes());
        for (var p : SmeltingMetalMod.getServer().getPlayerList().getPlayers()) {
            p.connection.send(packet);
        }
    }

    public static void removeBlockCraftingRecipes(RecipeManager recipeManager) {
        LOGGER.info("Removing vanilla block crafting recipes...");
        List<ResourceLocation> toRemove = recipeManager.getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.CRAFTING)
                .filter(r -> r instanceof ShapedRecipe)
                .filter(r -> {
                    ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(r.getResultItem(SmeltingMetalMod.getServer().registryAccess()).getItem());
                    if (resultId == null) return false;
                    String path = resultId.getPath();
                    if (!path.endsWith("_block") || path.startsWith("raw_")) return false;
                    String metalKey = path.substring(0, path.length() - 6);
                    return ModMetals.getMetalProperties(metalKey).isPresent();
                })
                .map(Recipe::getId)
                .toList();

        int removedCount = 0;
        for (ResourceLocation id : toRemove) {
            if (RecipeUtils.removeRecipeInManager(recipeManager, id)) {
                removedCount++;
            }
        }
        LOGGER.info("Successfully removed {} block crafting recipes.", removedCount);
    }

    private static void removeExistingMetalMeltingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        int removedCount = 0;
        List<Recipe<?>> recipesToProcess = new ArrayList<>(recipeManager.getRecipes());

        for (Recipe<?> recipe : recipesToProcess) {
            String metalKey = getMetalKeyFromRecipe(registryAccess, recipe);
            if (metalKey != null && !RecipeUtils.isInputBlacklisted(recipe)) {
                ItemStack moltenResult = ModItems.getMoltenMetalStack(metalKey);
                if (!moltenResult.isEmpty()) {
                    RecipeUtils.removeRecipeInManager(recipeManager, recipe.getId());
                    removedCount++;
                }
            }
        }
        LOGGER.info("Total metal smelting recipes removed: {}", removedCount);
    }

    private static @Nullable String getMetalKeyFromRecipe(RegistryAccess registryAccess, Recipe<?> recipe) {
        if (!Arrays.asList(RecipeType.SMELTING, RecipeType.BLASTING).contains(recipe.getType()) || !(recipe instanceof AbstractCookingRecipe)) {
            return null;
        }

        ItemStack resultStack = recipe.getResultItem(registryAccess);
        if (resultStack.isEmpty()) {
            return null;
        }

        ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(resultStack.getItem());
        if (resultId == null) {
            return null;
        }

        return RecipeUtils.getMetalKeyFromItem(resultId);
    }

    private static void addReverseNuggetCraftingRecipes(RecipeManager recipeManager) {
        int reverseRecipesAdded = 0;

        for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
            // Skip if this metal doesn't have a nugget item
            if (metalProps.nuggetId() == null) {
                continue;
            }

            // Get the nugget item
            Item nuggetItem = ForgeRegistries.ITEMS.getValue(metalProps.nuggetId());
            if (nuggetItem == null || nuggetItem == Items.AIR) {
                continue;
            }

            Item inputItem = null;
            String recipeIdSuffix = null;

            // Use the dedicated method to get the metal's ID for the recipe path
            // This is the key fix that prevents the colon from being included in the path
            String metalKey = metalProps.id().split(":")[1];

            // Try to get crushed metal first if Create is loaded, otherwise use raw metal
            if (ModList.get().isLoaded("create") && metalProps.crushedId() != null) {
                Item crushedItem = ForgeRegistries.ITEMS.getValue(metalProps.crushedId());
                if (crushedItem != null && crushedItem != Items.AIR) {
                    inputItem = crushedItem;
                    recipeIdSuffix = "nuggets_from_crushed_" + metalKey;
                }
            }

            // If we don't have a crushed item or Create isn't loaded, try raw metal
            if (inputItem == null && metalProps.rawId() != null) {
                Item rawItem = ForgeRegistries.ITEMS.getValue(metalProps.rawId());
                if (rawItem != null && rawItem != Items.AIR) {
                    inputItem = rawItem;
                    recipeIdSuffix = "nuggets_from_raw_" + metalKey;
                }
            }

            // If we found a valid input, create and add the recipe
            if (inputItem != null) {
                ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, recipeIdSuffix);
                Ingredient ingredient = Ingredient.of(inputItem);
                ItemStack outputStack = new ItemStack(nuggetItem, 9);

                ShapelessRecipe recipe = new ShapelessRecipe(
                        recipeId,
                        "",
                        CraftingBookCategory.MISC,
                        outputStack,
                        NonNullList.of(Ingredient.EMPTY, ingredient)
                );

                RecipeUtils.replaceRecipeInManager(recipeManager, recipeId, recipe);
                reverseRecipesAdded++;

                LOGGER.debug("Added reverse nugget recipe for {}: 1x {} -> 9x {}",
                        metalKey,
                        new ItemStack(inputItem).getHoverName().getString(),
                        outputStack.getHoverName().getString()
                );
            }
        }

        if (reverseRecipesAdded > 0) {
            LOGGER.info("Added {} reverse nugget crafting recipes.", reverseRecipesAdded);
        }
    }


    private static void replaceNuggetCraftingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        int nuggetReplaced = 0;
        List<Recipe<?>> allCraftingRecipes = recipeManager.getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.CRAFTING)
                .toList();

        for (Recipe<?> recipe : allCraftingRecipes) {
            // Check if the recipe is a 3x3 grid
            if (!(recipe instanceof ShapedRecipe shaped) || shaped.getWidth() != 3 || shaped.getHeight() != 3) {
                continue;
            }

            // verify that the result is a metal
            ItemStack result = shaped.getResultItem(registryAccess);
            if (result.isEmpty()) continue;

            ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(result.getItem());
            if (resultId == null) continue;

            String metalKey = ModMetals.getMetalId(resultId);
            if (metalKey == null) continue;

            // Get metal properties to access the correct item IDs
            MetalProperties metalProps = ModMetals.getMetalProperties(metalKey).orElse(null);
            if (metalProps == null) continue;

            // Skip if this metal doesn't have a nugget item
            if (metalProps.nuggetId() == null) {
                continue;
            }

            // Check if the recipe is a 3x3 grid of nuggets
            Item nuggetItem = ForgeRegistries.ITEMS.getValue(metalProps.nuggetId());
            if (nuggetItem == null || nuggetItem == Items.AIR) {
                continue;
            }

            ItemStack nuggetStack = new ItemStack(nuggetItem);
            boolean allNuggets = shaped.getIngredients().stream()
                    .allMatch(ing -> !ing.isEmpty() && ing.test(nuggetStack));

            if (allNuggets) {
                // Determine the output item - use crushed metal if available and Create mod is enabled, otherwise use raw metal
                Item outputItem = null;
                boolean useCrushed = ModList.get().isLoaded("create") && metalProps.crushedId() != null;

                if (useCrushed) {
                    outputItem = ForgeRegistries.ITEMS.getValue(metalProps.crushedId());
                    if (outputItem == null || outputItem == Items.AIR) {
                        LOGGER.debug("Crushed item not found for metal: {}, falling back to raw", metalKey);
                        useCrushed = false;
                    }
                }

                if (!useCrushed) {
                    outputItem = ForgeRegistries.ITEMS.getValue(metalProps.rawId());
                    if (outputItem == null || outputItem == Items.AIR) {
                        LOGGER.warn("Raw item not found for metal: {}", metalKey);
                        continue;
                    }
                }

                // Create and register the new recipe with the determined output
                ItemStack newResult = new ItemStack(outputItem);
                ShapedRecipe newRecipe = new ShapedRecipe(
                        recipe.getId(),
                        shaped.getGroup(),
                        shaped.category(),
                        shaped.getWidth(),
                        shaped.getHeight(),
                        shaped.getIngredients(),
                        newResult
                );

                // Replace the original recipe with our new version
                RecipeUtils.replaceRecipeInManager(recipeManager, recipe.getId(), newRecipe);
                nuggetReplaced++;

                LOGGER.debug("Replaced nugget recipe for {} with {}",
                        metalKey,
                        useCrushed ? "crushed metal" : "raw metal");
            }
        }
        LOGGER.info("Replaced {} nugget crafting recipes with {} output.", nuggetReplaced, ModList.get().isLoaded("create") ? "crushed/raw ore" : "raw ore");
    }

    private static void removeExistingCrushingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        int removedCount = 0;
        List<Recipe<?>> recipesToProcess = new ArrayList<>(recipeManager.getRecipes());

        for (Recipe<?> recipe : recipesToProcess) {
            ResourceLocation recipeId = recipe.getId();
            // Only process Create crushing recipes
            if (recipeId.getNamespace().equals("create") && recipeId.getPath().contains("crushing")) {
                // Check if the recipe is for a metal we handle
                String metalKey = getMetalKeyFromRecipe(registryAccess, recipe);
                if (metalKey != null && ModMetals.doesMetalExist(metalKey)) {
                    if (RecipeUtils.removeRecipeInManager(recipeManager, recipeId)) {
                        removedCount++;
                    }
                }
            }
        }

        LOGGER.info("Removed {} Create crushing recipes for metals.", removedCount);
    }

    private static void addNewCrushingRecipes(RecipeManager recipeManager) {

        int crushingRecipesAdded = 0;

        for (Item item : ForgeRegistries.ITEMS) {
            // skip if the item is blacklisted
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || RecipeUtils.isInputBlacklisted(new ItemStack(item))) {
                continue;
            }

            // Check if this is a metal item
            String metalKey = RecipeUtils.getMetalKeyFromItem(itemId);
            if (metalKey == null || !ModMetals.doesMetalExist(metalKey)) {
                continue;
            }

            // Get the crushed item and create a stack
            Item crushedItem = ForgeRegistries.ITEMS.getValue(ModMetals.getMetalProperties(metalKey).get().crushedId());
            if (crushedItem == null || crushedItem == Items.AIR) {
                LOGGER.warn("Crushed item for metal {} does not exist", metalKey);
                continue;
            }
            ItemStack resultStack = new ItemStack(crushedItem);

            // Add new crushing recipes
            RecipeUtils.createAndAddCrushingRecipe(recipeManager, item, resultStack);
            crushingRecipesAdded += 1;
        }
        LOGGER.info("Added {} new crushing recipes for metals.", crushingRecipesAdded);
    }

    private static void addNewMetalMeltingRecipes(RecipeManager recipeManager) {
        // Add raw metal block smelting recipes using registered metals
        int rawBlockAdded = 0;

        // Iterate through all registered metals
        for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
            // Get the raw block ID from MetalProperties
            ResourceLocation rawBlockId = metalProps.rawBlockId();

            // Get the molten metal block stack
            ItemStack moltenBlock = ModItems.getMoltenMetalBlockStack(metalProps.id());
            if (moltenBlock.isEmpty()) {
                LOGGER.warn("Failed to create molten metal block for {}", metalProps.id());
                continue;
            }

            // Add smelting and blasting recipes for the raw block
            Item rawBlockItem = ForgeRegistries.ITEMS.getValue(rawBlockId);
            if (rawBlockItem != null && rawBlockItem != Items.AIR) {
                RecipeUtils.createAndAddCookingRecipe(recipeManager, rawBlockItem, moltenBlock, RecipeType.SMELTING, 200, 0.7f);
                RecipeUtils.createAndAddCookingRecipe(recipeManager, rawBlockItem, moltenBlock, RecipeType.BLASTING, 100, 0.7f);
                rawBlockAdded += 2;
            }
        }
        LOGGER.info("Added {} new raw block molten recipes.", rawBlockAdded);

        // Add smelting recipes for other metal items (ingots, nuggets, etc.)
        int metalItemAdded = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || RecipeUtils.isInputBlacklisted(new ItemStack(item))) {
                continue;
            }

            // Skip if the item is already used in a cooking recipe
            boolean hasCookingRecipe = recipeManager.getRecipes().stream()
                    .filter(r -> r instanceof AbstractCookingRecipe)
                    .anyMatch(r -> r.getIngredients().get(0).test(new ItemStack(item)));
            if (hasCookingRecipe) {
                continue;
            }

            // Check if this is a metal item we should process
            String metalKey = RecipeUtils.getMetalKeyFromItem(itemId);
            if (metalKey == null || !ModMetals.doesMetalExist(metalKey)) {
                continue;
            }

            // Get the molten metal stack for this metal
            ItemStack resultStack = ModItems.getMoltenMetalStack(metalKey);
            if (resultStack.isEmpty()) {
                continue;
            }

            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, resultStack, RecipeType.SMELTING, 200, 0.1f);
            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, resultStack, RecipeType.BLASTING, 100, 0.1f);
            metalItemAdded += 2;
        }
        LOGGER.info("Added {} new molten recipes for metal items.", metalItemAdded);
    }
}