package com.smeltingmetal.recipes;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModBlocks;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import com.smeltingmetal.objects.generic.MetalItem;
import com.smeltingmetal.objects.mold.BlockMoldItem;
import com.smeltingmetal.objects.mold.ItemMold;
import com.smeltingmetal.utils.MetalUtils;
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
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        boolean shouldModifyNugget = MetalsConfig.CONFIG.enableNuggetRecipeReplacement.get();
        boolean shouldRemoveResultRecipes = MetalsConfig.CONFIG.enableResultRecipeRemoval.get();


        List<Recipe<?>> metalRecipes = new ArrayList<>(recipeManager.getRecipes()).stream()
                .filter(r -> isResourceMetal(RecipeUtils.getRecipeResultLocation(registryAccess, r)))
                .toList();
        List<ResourceLocation> metalItems = ForgeRegistries.ITEMS.getKeys().stream()
                .filter(RecipeProcessor::isResourceMetal)
                .filter(RecipeUtils::isItemNotBlacklisted)
                .toList();
        List<Recipe<?>> recipesToRemove = new ArrayList<>();

        addMoldCraftingRecipes(recipeManager);

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

        if (shouldModifyNugget) {
            List<Recipe<?>> nuggetCraftingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> r instanceof ShapedRecipe shaped
                            && shaped.getWidth() == 3 && shaped.getHeight() == 3
                            && doAllIngriedientsMatchPattern(shaped, "nugget")
                    ).toList();
            recipesToRemove.addAll(nuggetCraftingRecipesToRemove);
            addNuggetCraftingRecipes(recipeManager);
        }

        // Remove recipes that produce items from MetalProperties
        if (shouldRemoveResultRecipes) {
            Set<ResourceLocation> metalResultIds = ModMetals.getMetalPropertiesMap().values().stream()
                    .flatMap(mp -> Stream.concat(
                            mp.itemResults().values().stream(),
                            mp.blockResults().values().stream()
                    ))
                    .collect(Collectors.toSet());

            List<Recipe<?>> resultCraftingRecipesToRemove = metalRecipes.stream()
                    .filter(r -> {
                        ResourceLocation resultId = ForgeRegistries.ITEMS.getKey(r.getResultItem(registryAccess).getItem());
                        return resultId != null && metalResultIds.contains(resultId);
                    })
                    .toList();
            recipesToRemove.addAll(resultCraftingRecipesToRemove);
        }

        recipesToRemove.forEach(recipe -> RecipeUtils.removeRecipeInManager(recipeManager, recipe.getId()));
    }

    private static void addMoldCraftingRecipes(RecipeManager recipeManager) {
        List<Item> itemMolds = List.of(ModItems.ITEM_MOLD_HARDENED.get(), ModItems.ITEM_MOLD_NETHERITE.get());
        List<Item> blockMolds = List.of(ModBlocks.BLOCK_MOLD_HARDENED_ITEM.get(), ModBlocks.BLOCK_MOLD_NETHERITE_ITEM.get());

        Map<String, RegistryObject<Item>> itemMoldsClay = ModItems.ITEM_MOLDS_CLAY;
        Map<String, RegistryObject<Item>> blockMoldsClay = ModBlocks.BLOCK_MOLDS_CLAY;

        for (boolean isBlock : List.of(false, true)) {
            for (var entry : (isBlock ? blockMoldsClay : itemMoldsClay).entrySet()) {
                String shape = entry.getKey();
                Item inputMold = entry.getValue().get();
                for (Item mold : (isBlock ? blockMolds : itemMolds)) {
                    ItemStack resultStack = new ItemStack(mold);
                    MetalUtils.setShapeToStack(resultStack, shape, isBlock);
                    boolean isNetherite = (mold instanceof ItemMold itemMold && itemMold.getMaterialType() == MaterialType.NETHERITE)
                            || (mold instanceof BlockMoldItem blockMold && blockMold.getMaterialType() == MaterialType.NETHERITE);
                    if (isNetherite) {
                        createAndAddMoldCraftingNetheriteUpgrade(recipeManager, inputMold, resultStack);
                        createAndAddMoldSmithingNetheriteUpgrade(recipeManager, inputMold, resultStack);
                    } else {
                        if (isBlock) {
                            LOGGER.info("Adding block mold recipe for " + mold);
                        }
                        createAndAddCookingRecipe(recipeManager, inputMold, resultStack, RecipeType.SMELTING, 200, 1.4f);
                    }
                }
            }
        }

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
        return MetalUtils.getAllMetalProperties().keySet().stream()
                .anyMatch(metalKey -> result.getPath().contains(metalKey));
    }

    private static void addNuggetCraftingRecipes(RecipeManager recipeManager) {
        for (MetalProperties metalProps : MetalUtils.getAllMetalProperties().values()) {
            // setup input and output items
            boolean useCrushed = ModList.get().isLoaded("create") && metalProps.crushed() != null;
            ResourceLocation nuggetGroupLoc = useCrushed ? metalProps.crushed() : metalProps.raw();
            if (metalProps.nugget() == null || nuggetGroupLoc == null) continue;
            Item nuggetGroup = ForgeRegistries.ITEMS.getValue(nuggetGroupLoc);
            Item nuggetItem = ForgeRegistries.ITEMS.getValue(metalProps.nugget());
            if (nuggetGroup == null || nuggetItem == null) continue;
            if (nuggetGroup instanceof MetalItem || nuggetGroup instanceof MetalBlockItem) {
                ItemStack stack = new ItemStack(nuggetGroup);
                MetalUtils.setMetalToStack(stack, metalProps.name());
            }
            if (nuggetItem instanceof MetalItem || nuggetItem instanceof MetalBlockItem) {
                ItemStack stack = new ItemStack(nuggetItem);
                MetalUtils.setMetalToStack(stack, metalProps.name());
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

        String metalName = MetalUtils.getMetalKeyFromString(singleItem.toString());
        ItemStack groupStack = new ItemStack(groupItem, 1);
        if (metalName != null && (groupItem instanceof MetalItem || groupItem instanceof MetalBlockItem)) {
            MetalUtils.setMetalToStack(groupStack, metalName);
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

        String metalName = MetalUtils.getMetalKeyFromString(groupItem.toString());
        ItemStack singleStack = new ItemStack(singleItem, 9);
        if (metalName != null && (singleItem instanceof MetalItem || singleItem instanceof MetalBlockItem)) {
            MetalUtils.setMetalToStack(singleStack, metalName);
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

    private static void addNewCrushingRecipes(RecipeManager recipeManager, List<ResourceLocation> metalItems) {
        for (ResourceLocation itemId : metalItems) {
            String metalKey = MetalUtils.getMetalKeyFromString(itemId.getPath());
            if (metalKey == null) continue;
            MetalProperties metalProps = MetalUtils.getMetalProperties(metalKey);
            if (metalProps == null) continue;
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem == null || inputItem == Items.AIR) continue;
            Item resultItem = ForgeRegistries.ITEMS.getValue(metalProps.crushed());
            if (resultItem == null || resultItem == Items.AIR) continue;
            boolean isBlock = MetalUtils.isItemBlock(itemId);
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
            String metalKey = MetalUtils.getMetalKeyFromString(itemId.getPath());
            if (metalKey == null) continue;
            MetalProperties metalProps = MetalUtils.getMetalProperties(metalKey);
            if (metalProps == null) continue;
            boolean isBlock = metalProps.ingot() == null || (metalProps.block() != null && MetalUtils.isItemBlock(itemId));

            // Create an input item
            Item inputItem = ForgeRegistries.ITEMS.getValue(itemId);
            if (inputItem == null || inputItem == Items.AIR) continue;

            // Create result stack
            Item resultItem = isBlock ? ModBlocks.MOLTEN_METAL_BLOCK_ITEM.get() : ModItems.MOLTEN_METAL_ITEM.get();
            ItemStack resultStack = new ItemStack(resultItem);
            MetalUtils.setMetalToStack(resultStack, metalProps.name());

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

    private static void createAndAddMoldCraftingNetheriteUpgrade(RecipeManager recipeManager, Item input, ItemStack output) {
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(input);
        if (inputId == null) return;

        // Create recipe ID based on input item
        String idName = String.format("smithing_%s_%s", inputId.getNamespace(), inputId.getPath());
        ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, idName);

        // Set up ingredients for the cross pattern
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient netheriteItem = Ingredient.of(input instanceof ItemMold ? Items.NETHERITE_SCRAP : Items.NETHERITE_INGOT);
        Ingredient moldIngriedient = Ingredient.of(input);

        // Create cross pattern (indices for 3x3 grid: 0-8)
        ingredients.set(1, netheriteItem);  // Top center
        ingredients.set(3, netheriteItem);  // Middle left
        ingredients.set(4, moldIngriedient); // Center
        ingredients.set(5, netheriteItem);  // Middle right
        ingredients.set(7, netheriteItem);  // Bottom center

        // Create and register the recipe
        ShapedRecipe recipe = new ShapedRecipe(
                recipeId,
                "", // Group name (empty for no group)
                CraftingBookCategory.MISC,
                3, 3, // 3x3 grid
                ingredients,
                output
        );

        RecipeUtils.createInRecipeInManager(recipeManager, recipeId, recipe);
    }

    private static void createAndAddMoldSmithingNetheriteUpgrade(RecipeManager recipeManager, Item input, ItemStack output) {
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(input);
        if (inputId == null) return;

        String idName = String.format("smithing_%s_%s", inputId.getNamespace(), inputId.getPath());
        ResourceLocation recipeId = new ResourceLocation(SmeltingMetalMod.MODID, idName);

        Ingredient template = Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        Ingredient base = Ingredient.of(input);
        Ingredient addition = Ingredient.of(Items.NETHERITE_INGOT);

        // Create the smithing recipe
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                recipeId,
                template,
                base,
                addition,
                output
        );

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
}