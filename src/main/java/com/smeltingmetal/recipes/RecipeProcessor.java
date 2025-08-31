package com.smeltingmetal.recipes;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModBlocks;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.generic.MetalBlockItem;
import com.smeltingmetal.items.generic.MetalItem;
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

        modifyRecipes(recipeManager, registryAccess);

        syncRecipesToPlayers();
        LOGGER.info("===== RECIPE MODIFICATIONS COMPLETE =====");
    }

    private static void modifyRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        boolean isCreateLoaded = ModList.get().isLoaded("create");
        boolean shouldModifyCrushing = isCreateLoaded && MetalsConfig.CONFIG.enableCrushingRecipeReplacement.get();
        boolean shouldModifySmelting = MetalsConfig.CONFIG.enableMeltingRecipeReplacement.get();

        List<Recipe<?>> metalRecipes = new ArrayList<>(recipeManager.getRecipes()).stream()
                .filter(r -> isResourceMetal(RecipeUtils.getRecipeResultLocation(registryAccess, r)))
                .toList();
        List<ResourceLocation> metalItems = ForgeRegistries.ITEMS.getKeys().stream()
                .filter(RecipeProcessor::isResourceMetal)
                .filter(RecipeUtils::isItemNotBlacklisted)
                .toList();
        List<Recipe<?>> recipesToRemove = new ArrayList<>();


        if (shouldModifySmelting) {
            List<Recipe<?>> metalMeltingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> Arrays.asList(RecipeType.SMELTING, RecipeType.BLASTING).contains(r.getType()))
                    .filter(RecipeUtils::isInputNotBlacklisted)
                    .toList();
            recipesToRemove.addAll(metalMeltingRecipesToRemove);
            addNewMetalMeltingRecipes(recipeManager, metalItems);
        }

        if (shouldModifyCrushing) {
            List<Recipe<?>> metalCrushingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> r instanceof CrushingRecipe)
                    .filter(RecipeUtils::isInputNotBlacklisted)
                    .toList();
            recipesToRemove.addAll(metalCrushingRecipesToRemove);
            addNewCrushingRecipes(recipeManager, metalItems);
        }

        if (MetalsConfig.CONFIG.enableNuggetRecipeReplacement.get()) {
            List<Recipe<?>> nuggetCraftingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> r instanceof ShapedRecipe shaped
                            && shaped.getWidth() == 3 && shaped.getHeight() == 3
                            && doAllIngriedientsMatchPattern(shaped, "nugget")
                    ).toList();
            recipesToRemove.addAll(nuggetCraftingRecipesToRemove);
            addNuggetCraftingRecipes(recipeManager);
        }

//        addRawCraftingRecipes(recipeManager); // TODO doesnt transfer metal type

        recipesToRemove.forEach(recipe -> RecipeUtils.removeRecipeInManager(recipeManager, recipe.getId()));
    }


    private static boolean doAllIngriedientsMatchPattern(ShapedRecipe shaped, String pattern) {
        return shaped.getIngredients().stream().allMatch(ing ->
                !ing.isEmpty() && Arrays.stream(ing.getItems()).allMatch(i -> doesItemStackMatchPattern(i, pattern)));
    }

    private static boolean doesItemStackMatchPattern(ItemStack stack, String pattern) {
        if (stack.isEmpty()) return false;
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return loc != null && loc.getPath().contains(pattern);
    }

    private static boolean isResourceMetal(ResourceLocation result) {
        if (result == null) return false;
        return ModMetals.getAllMetalProperties().keySet().stream()
                .anyMatch(metalKey -> result.getPath().contains(metalKey));
    }

    private static void addNuggetCraftingRecipes(RecipeManager recipeManager) {
        for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
            // setup input and output items
            boolean useCrushed = ModList.get().isLoaded("create") && metalProps.crushed() != null;
            ResourceLocation nuggetGroupLoc = useCrushed ? metalProps.crushed() : metalProps.raw();
            if (metalProps.nugget() == null || nuggetGroupLoc == null) continue;
            Item nuggetGroup = ForgeRegistries.ITEMS.getValue(nuggetGroupLoc);
            Item nuggetItem = ForgeRegistries.ITEMS.getValue(metalProps.nugget());
            if (nuggetGroup == null || nuggetItem == null) continue;
            if (nuggetGroup instanceof MetalItem || nuggetGroup instanceof MetalBlockItem) {
                ItemStack stack = new ItemStack(nuggetGroup);
                ModMetals.setMetalTypeToStack(stack, metalProps.name());
            }
            if (nuggetItem instanceof MetalItem || nuggetItem instanceof MetalBlockItem) {
                ItemStack stack = new ItemStack(nuggetItem);
                ModMetals.setMetalTypeToStack(stack, metalProps.name());
            }

            // create shapeless recipe
            create9ItemsFrom1(recipeManager, nuggetItem, nuggetGroup, nuggetGroupLoc.getPath(), metalProps.nugget().getPath());

            // create shaped recipe
            create1ItemFrom9(recipeManager, nuggetItem, nuggetGroup, nuggetGroupLoc.getPath(), metalProps.nugget().getPath());
        }
    }

    private static void create1ItemFrom9(RecipeManager recipeManager, Item singleItem, Item groupItem, String singleName, String groupName) {
        String shapedRecipeIdSuffix = groupName + "_from_" + singleName;
        ResourceLocation shapedRecipeId = new ResourceLocation(SmeltingMetalMod.MODID, shapedRecipeIdSuffix);
        NonNullList<Ingredient> nuggetIngs = NonNullList.withSize(9, Ingredient.EMPTY);
        for (int i = 0; i < 9; i++) {
            nuggetIngs.set(i, Ingredient.of(singleItem));
        }

        String metalName = RecipeUtils.getMetalKeyFromString(singleItem.toString());
        ItemStack groupStack = new ItemStack(groupItem, 1);
        if (metalName != null && (groupItem instanceof MetalItem || groupItem instanceof MetalBlockItem)) {
            ModMetals.setMetalTypeToStack(groupStack, metalName);
        }

        ShapedRecipe shapedRecipe = new ShapedRecipe(
                shapedRecipeId,
                "",
                CraftingBookCategory.MISC,
                3, 3,
                nuggetIngs,
                groupStack
        );
        RecipeUtils.createInRecipeInManager(recipeManager, shapedRecipeId, shapedRecipe);
    }

    private static void create9ItemsFrom1(RecipeManager recipeManager, Item singleItem, Item groupItem, String singleName, String groupName) {
        String shapelessRecipeIdSuffix = singleName + "_from_" + groupName;
        ResourceLocation shapelessRecipeId = new ResourceLocation(SmeltingMetalMod.MODID, shapelessRecipeIdSuffix);

        String metalName = RecipeUtils.getMetalKeyFromString(groupItem.toString());
        ItemStack singleStack = new ItemStack(singleItem, 9);
        if (metalName != null && (singleItem instanceof MetalItem || singleItem instanceof MetalBlockItem)) {
            ModMetals.setMetalTypeToStack(singleStack, metalName);
        }

        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(
                shapelessRecipeId,
                "",
                CraftingBookCategory.MISC,
                singleStack,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(groupItem))
        );
        RecipeUtils.createInRecipeInManager(recipeManager, shapelessRecipeId, shapelessRecipe);
    }

    private static void addRawCraftingRecipes(RecipeManager recipeManager) {

        Item rawItem = ModItems.RAW_METAL_ITEM.get();
        Item rawBlock = ModBlocks.RAW_METAL_BLOCK_ITEM.get();
        Item nugget = ModItems.METAL_NUGGET.get();

        create1ItemFrom9(recipeManager, rawItem, rawBlock, "raw_metal", "raw_metal_block");
        create9ItemsFrom1(recipeManager, rawItem, rawBlock, "raw_metal", "raw_metal_block");
        create1ItemFrom9(recipeManager, nugget, rawItem, "nugget", "raw_metal");
        create9ItemsFrom1(recipeManager, nugget, rawItem, "nugget", "raw_metal");
    }

    private static void addNewCrushingRecipes(RecipeManager recipeManager, List<ResourceLocation> metalItems) {
        for (ResourceLocation itemId : metalItems) {
            String metalKey = RecipeUtils.getMetalKeyFromString(itemId.getPath());
            if (metalKey == null) continue;
            MetalProperties metalProps = ModMetals.getMetalProperties(metalKey);
            if (metalProps == null) continue;
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem == null || inputItem == Items.AIR) continue;
            Item resultItem = ForgeRegistries.ITEMS.getValue(metalProps.crushed());
            if (resultItem == null || resultItem == Items.AIR) continue;
            boolean isBlock = isItemBlock(itemId);
            ItemStack resultStack = new ItemStack(resultItem, isBlock ? 9 : 1);
            if (resultStack.isEmpty()) continue;

            createAndAddCrushingRecipe(recipeManager, inputItem, resultStack);
        }
    }

    private static void createAndAddCrushingRecipe(RecipeManager recipeManager, Item input, ItemStack result) {
        try {
            String recipeName = "crushing/" + ForgeRegistries.ITEMS.getKey(input).getPath() + "_to_" + ForgeRegistries.ITEMS.getKey(result.getItem()).getPath();
            ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, recipeName);
            ProcessingRecipeBuilder<CrushingRecipe> builder = new ProcessingRecipeBuilder<>(CrushingRecipe::new, recipeId);
            builder.withItemIngredients(Ingredient.of(input))
                    .output(result)
                    .output(.1f, result)  // Secondary output with 10% chance
                    .build();
            CrushingRecipe recipe = builder.build();
            RecipeUtils.createInRecipeInManager(recipeManager, recipeId, recipe);
        } catch (Exception e) {
            LOGGER.error("Failed to create crushing recipe for {} -> {}: {}",
                    ForgeRegistries.ITEMS.getKey(input),
                    ForgeRegistries.ITEMS.getKey(result.getItem()),
                    e.getMessage());
        }
    }

    private static void addNewMetalMeltingRecipes(RecipeManager recipeManager, List<ResourceLocation> metalItems) {
        for (ResourceLocation itemId : metalItems) {
            String metalKey = RecipeUtils.getMetalKeyFromString(itemId.getPath());
            if (metalKey == null) continue;
            MetalProperties metalProps = ModMetals.getMetalProperties(metalKey);
            if (metalProps == null) continue;
            boolean isBlock = metalProps.ingot() == null || (metalProps.block() != null && isItemBlock(itemId));

            // Create an input item
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem == null || inputItem == Items.AIR) continue;

            // Create result stack
            Item resultItem = isBlock ? ModBlocks.MOLTEN_METAL_BLOCK_ITEM.get() : ModItems.MOLTEN_METAL_ITEM.get();
            ItemStack resultStack = new ItemStack(resultItem);
            ModMetals.setMetalTypeToStack(resultStack, metalProps.name());

            int time = isBlock ? 400 : 200;
            float xp = isBlock ? 1.4f : 0.7f;
            createAndAddCookingRecipe(recipeManager, inputItem, resultStack, RecipeType.SMELTING, time, xp);
            createAndAddCookingRecipe(recipeManager, inputItem, resultStack, RecipeType.BLASTING, time / 2, xp);
        }
    }

    private static void createAndAddCookingRecipe(RecipeManager recipeManager, Item input, ItemStack output, RecipeType<?> type, int time, float xp) {
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(input);
        if (inputId == null) return;

        String typeName = type == RecipeType.SMELTING ? "smelting" : "blasting";
        String idName = String.format("%s_%s_%s", typeName, inputId.getNamespace(), inputId.getPath());

        ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, idName);
        Ingredient ingredient = Ingredient.of(input);

        AbstractCookingRecipe recipe = type == RecipeType.SMELTING ?
                new SmeltingRecipe(recipeId, "", CookingBookCategory.MISC, ingredient, output, xp, time) :
                new BlastingRecipe(recipeId, "", CookingBookCategory.MISC, ingredient, output, xp, time / 2);

        RecipeUtils.createInRecipeInManager(recipeManager, recipeId, recipe);
    }

    private static void syncRecipesToPlayers() {
        if (SmeltingMetalMod.getServer() == null) return;
        var packet = new ClientboundUpdateRecipesPacket(
                SmeltingMetalMod.getServer().getRecipeManager().getRecipes());
        for (var p : SmeltingMetalMod.getServer().getPlayerList().getPlayers()) {
            p.connection.send(packet);
        }
    }

    private static boolean isItemBlock(ResourceLocation itemId) {
        if (itemId == null) return false;
        String path = itemId.getPath();
        return MetalsConfig.CONFIG.blockKeywords.get().stream().anyMatch(path::contains);
    }
}