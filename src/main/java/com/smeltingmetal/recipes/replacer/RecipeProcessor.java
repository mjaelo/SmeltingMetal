package com.smeltingmetal.recipes.replacer;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.MetalsConfig;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

import static com.smeltingmetal.SmeltingMetalMod.MODID;

/**
 * Called when the server starts. This is the entry point for all recipe modifications.
 */
@Mod.EventBusSubscriber(modid = MODID)
public class RecipeProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RecipeManager recipeManager = SmeltingMetalMod.getRecipeManager();
        if (recipeManager == null) {
            LOGGER.warn(MODID + " RecipeManager is not available, skipping recipe modifications.");
            return;
        }

        MinecraftServer server = event.getServer();
        RegistryAccess registryAccess = server.registryAccess();

        if (MetalsConfig.CONFIG.enableMetalMelting.get()) {
            removeExistingMetalMeltingRecipes(recipeManager, registryAccess);
            removeBlockCraftingRecipes(recipeManager);
            addNewMetalMeltingRecipes(recipeManager);
        }

        // handle special recipes
        if (MetalsConfig.CONFIG.enableNuggetRecipeReplacement.get()) {
            replaceNuggetCraftingRecipes(recipeManager, registryAccess);
        }

        LOGGER.info("===== RECIPE MODIFICATIONS COMPLETE =====");
    }

    public static void removeBlockCraftingRecipes(RecipeManager recipeManager) {
        LOGGER.info("Removing vanilla block crafting recipes...");
        List<ResourceLocation> toRemove = recipeManager.getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.CRAFTING)
                .filter(r -> r instanceof ShapedRecipe)
                .filter(r -> {
                    ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(r.getResultItem(SmeltingMetalMod.getServer().registryAccess()).getItem());
                    return resultId != null && resultId.getPath().endsWith("_block");
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

    private static void replaceNuggetCraftingRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        int nuggetReplaced = 0;
        List<Recipe<?>> allCraftingRecipes = recipeManager.getRecipes().stream()
                .filter(r -> r.getType() == RecipeType.CRAFTING)
                .toList();

        for (Recipe<?> recipe : allCraftingRecipes) {
            if (!(recipe instanceof ShapedRecipe shaped) || shaped.getWidth() != 3 || shaped.getHeight() != 3) {
                continue;
            }

            ItemStack result = shaped.getResultItem(registryAccess);
            if (result.isEmpty()) continue;
            ResourceLocation ingotId = ForgeRegistries.ITEMS.getKey(result.getItem());
            if (ingotId == null) continue;

            String metalKey = ModMetals.getMetalId(ingotId);
            if (metalKey == null) continue;

            // This is the corrected logic, using string manipulation from the original code
            String base = ingotId.getPath().replace("_ingot", "").replace("ingot", "");
            ResourceLocation nuggetId = new ResourceLocation(ingotId.getNamespace(), base + "_nugget");
            ResourceLocation rawOreId = new ResourceLocation(ingotId.getNamespace(), "raw_" + base);

            if (!ForgeRegistries.ITEMS.containsKey(rawOreId) || !ForgeRegistries.ITEMS.containsKey(nuggetId)) {
                continue;
            }

            ItemStack nuggetStack = new ItemStack(ForgeRegistries.ITEMS.getValue(nuggetId));
            boolean allNuggets = shaped.getIngredients().stream().allMatch(ing -> !ing.isEmpty() && ing.test(nuggetStack));

            if (allNuggets) {
                ItemStack newResult = new ItemStack(ForgeRegistries.ITEMS.getValue(rawOreId));
                ShapedRecipe newRecipe = new ShapedRecipe(
                        recipe.getId(), shaped.getGroup(), shaped.category(),
                        shaped.getWidth(), shaped.getHeight(),
                        shaped.getIngredients(), newResult
                );
                RecipeUtils.replaceRecipeInManager(recipeManager, recipe.getId(), newRecipe);
                nuggetReplaced++;
            }
        }
        LOGGER.info("Replaced {} nugget crafting recipes with raw ore output.", nuggetReplaced);
    }

    private static void addNewMetalMeltingRecipes(RecipeManager recipeManager) {
        // add raw metal block smelting recipes
        int rawBlockAdded = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || !itemId.getPath().startsWith("raw_") || !itemId.getPath().endsWith("_block")) {
                continue;
            }

            String metalKey = itemId.getPath().substring(4, itemId.getPath().length() - 6);
            ItemStack moltenBlock = ModItems.getMoltenMetalBlockStack(itemId.getNamespace() + ":" + metalKey);
            if (moltenBlock.isEmpty()) continue;

            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, moltenBlock, RecipeType.SMELTING, 200, 0.7f);
            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, moltenBlock, RecipeType.BLASTING, 100, 0.7f);
            rawBlockAdded += 2;
        }
        LOGGER.info("Added {} new raw block molten recipes.", rawBlockAdded);

        // add metal items smelting recipes
        int metalItemAdded = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null || RecipeUtils.isInputBlacklisted(new ItemStack(item))) continue;
            String metalKey = RecipeUtils.getMetalKeyFromItem(itemId);
            if (metalKey == null) continue;

            boolean hasCookingRecipe = recipeManager.getRecipes().stream()
                    .filter(r -> r instanceof AbstractCookingRecipe)
                    .anyMatch(r -> r.getIngredients().get(0).test(new ItemStack(item)));

            if (hasCookingRecipe) continue;

            ItemStack resultStack = ModItems.getMoltenMetalStack(metalKey);
            if (resultStack.isEmpty()) continue;

            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, resultStack, RecipeType.SMELTING, 200, 0.1f);
            RecipeUtils.createAndAddCookingRecipe(recipeManager, item, resultStack, RecipeType.BLASTING, 100, 0.1f);
            metalItemAdded += 2;
        }
        LOGGER.info("Added {} new molten recipes for metal items.", metalItemAdded);
    }
}