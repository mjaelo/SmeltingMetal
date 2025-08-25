package com.smeltingmetal.recipes.replacer;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.smeltingmetal.MetalsConfig;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.metalBlock.MoltenMetalBlockItem;
import com.smeltingmetal.items.metalIngot.MoltenMetalItem;
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

        // handle special recipes
        if (MetalsConfig.CONFIG.enableNuggetRecipeReplacement.get()) {
            List<Recipe<?>> nuggetCraftingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> r instanceof ShapedRecipe shaped
                            && shaped.getWidth() == 3 && shaped.getHeight() == 3
                            && doAllIngriedientsMatchPattern(shaped, "nugget")
                    ).toList();
            recipesToRemove.addAll(nuggetCraftingRecipesToRemove);
            addNuggetCraftingRecipes(recipeManager);
        }

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
                .map(metalKey -> metalKey.split(":")[1])
                .anyMatch(metalKey -> result.getPath().contains(metalKey));
    }

    private static void addNuggetCraftingRecipes(RecipeManager recipeManager) {
        for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
            // get result and input
            boolean useCrushed = ModList.get().isLoaded("create") && metalProps.crushedId() != null;
            ResourceLocation nuggetGroupLoc = useCrushed ? metalProps.crushedId() : metalProps.rawId();
            if (metalProps.nuggetId() == null || nuggetGroupLoc == null) {
                continue;
            }
            Item nuggetGroup = ForgeRegistries.ITEMS.getValue(nuggetGroupLoc);
            Item nuggetItem = ForgeRegistries.ITEMS.getValue(metalProps.nuggetId());

            if (nuggetGroup == null || nuggetItem == null) {
                continue;
            }

            // create shapeless recipe
            String shapelessRecipeIdSuffix = "nuggets_from_" + nuggetGroupLoc.getPath();
            ResourceLocation shapelessRecipeId = new ResourceLocation(SmeltingMetalMod.MODID, shapelessRecipeIdSuffix);
            ShapelessRecipe shapelessRecipe = new ShapelessRecipe(
                    shapelessRecipeId,
                    "",
                    CraftingBookCategory.MISC,
                    new ItemStack(nuggetItem, 9),
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(nuggetGroup))
            );
            RecipeUtils.createInRecipeInManager(recipeManager, shapelessRecipeId, shapelessRecipe);

            // create shaped recipe
            String shapedRecipeIdSuffix = nuggetGroupLoc.getPath() + "_from_nuggets";
            ResourceLocation shapedRecipeId = new ResourceLocation(SmeltingMetalMod.MODID, shapedRecipeIdSuffix);
            NonNullList<Ingredient> nuggetIngs = NonNullList.withSize(9, Ingredient.EMPTY);
            for (int i = 0; i < 9; i++) {
                nuggetIngs.set(i, Ingredient.of(nuggetItem));
            }
            ShapedRecipe shapedRecipe = new ShapedRecipe(
                    shapedRecipeId,
                    "",
                    CraftingBookCategory.MISC,
                    3, 3,
                    nuggetIngs,
                    new ItemStack(nuggetGroup, 1)
            );
            RecipeUtils.createInRecipeInManager(recipeManager, shapedRecipeId, shapedRecipe);
        }
    }

    private static void addNewCrushingRecipes(RecipeManager recipeManager, List<ResourceLocation> metalItems) {
        for (ResourceLocation itemId : metalItems) {
            // Check if this is a metal item
            String metalKey = RecipeUtils.getMetalKeyFromItem(itemId);
            if (metalKey == null) {
                continue;
            }

            // Get the molten metal block stack
            MetalProperties metalProps = ModMetals.getMetalProperties(metalKey).get();
            boolean isBlock = isItemBlock(itemId);
            Item resultItem = ForgeRegistries.ITEMS.getValue(metalProps.crushedId());
            if (resultItem == null || resultItem == Items.AIR) {
                continue;
            }
            ItemStack resultStack = new ItemStack(resultItem, isBlock ? 9 : 1);
            if (resultStack.isEmpty()) {
                continue;
            }

            // Add smelting and blasting recipes for the raw block
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem != null && inputItem != Items.AIR) {
                createAndAddCrushingRecipe(recipeManager, inputItem, resultStack);
            }
        }
    }

    private static void createAndAddCrushingRecipe(RecipeManager recipeManager, Item input, ItemStack result) {
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
            RecipeUtils.createInRecipeInManager(recipeManager, recipeId, recipe);
            LOGGER.info("Created crushing recipe for {}", recipeId);
        } catch (Exception e) {
            LOGGER.error("Failed to create crushing recipe for {} -> {}: {}",
                    ForgeRegistries.ITEMS.getKey(input),
                    ForgeRegistries.ITEMS.getKey(result.getItem()),
                    e.getMessage());
        }
    }

    private static void addNewMetalMeltingRecipes(RecipeManager recipeManager, List<ResourceLocation> metalItems) {
        // Iterate through all registered metals
        for (ResourceLocation itemId : metalItems) {
            // Check if this is a metal item
            String metalKey = RecipeUtils.getMetalKeyFromItem(itemId);
            if (metalKey == null) {
                continue;
            }

            // Get the molten metal block stack
            MetalProperties metalProps = ModMetals.getMetalProperties(metalKey).get();
            boolean isBlock = isItemBlock(itemId);
            ItemStack resultStack = isBlock
                    ? MoltenMetalBlockItem.createStack(metalProps.id())
                    : MoltenMetalItem.createStack(metalProps.id());
            if (resultStack.isEmpty()) {
                continue;
            }

            // Add smelting and blasting recipes for the raw block
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem != null && inputItem != Items.AIR) {
                int time = isBlock ? 400 : 200;
                float xp = isBlock ? 1.4f : 0.7f;
                createAndAddCookingRecipe(recipeManager, inputItem, resultStack, RecipeType.SMELTING, time, xp);
                createAndAddCookingRecipe(recipeManager, inputItem, resultStack, RecipeType.BLASTING, time / 2, xp);
            }
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
        LOGGER.info("Created {} recipe for {}",typeName, recipeId);
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
        if (itemId == null) {
            return false;
        }
        
        String path = itemId.getPath();
        return MetalsConfig.CONFIG.blockKeywords.get().stream()
                .anyMatch(keyword -> path.contains(keyword));
    }
}