package com.smeltingmetal.recipes.datagen;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Handles the generation of all custom recipes for the Smelting Metal mod.
 * This class is responsible for creating and registering all smelting and blasting recipes
 * for the various metals defined in the mod's configuration.
 */
public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // Make sure metals are initialized
        ModMetals.init();
        
        // Add recipes for each metal
        for (Map.Entry<String, MetalProperties> entry : ModMetals.getAllMetalProperties().entrySet()) {
            String metalType = entry.getKey();
            ResourceLocation ingotId = entry.getValue().ingotId();
            addMetalRecipes(pWriter, metalType, ingotId);
        }
    }


    private void addMetalRecipes(Consumer<FinishedRecipe> pWriter, String metalType, ResourceLocation ingotId) {
        // Get the molten metal item
        Item moltenMetalItem = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation(SmeltingMetalMod.MODID, "molten_" + metalType));
            
        if (moltenMetalItem == null) {
            // Log to console since we can't access LOGGER directly
            System.out.println("[SmeltingMetal] No molten metal item found for: " + metalType);
            return;
        }
        
        // Get the ingot item
        Item ingotItem = ForgeRegistries.ITEMS.getValue(ingotId);
        if (ingotItem == null) {
            // Log to console since we can't access LOGGER directly
            System.out.println("[SmeltingMetal] No ingot item found for: " + ingotId);
            return;
        }
        
        // Default experience value
        float experience = 0.7f; // Default experience value, can be adjusted per metal if needed
        
        // Create smelting recipe (200 ticks = 10 seconds)
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ingotItem),
                RecipeCategory.MISC,
                moltenMetalItem,
                experience,
                200)
            .unlockedBy("has_" + metalType + "_ingot", has(ingotItem))
            .save(pWriter, new ResourceLocation(SmeltingMetalMod.MODID, "smelting/molten_" + metalType + "_from_smelting"));
        
        // Create blasting recipe (100 ticks = 5 seconds)
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ingotItem),
                RecipeCategory.MISC,
                moltenMetalItem,
                experience,
                100)
            .unlockedBy("has_" + metalType + "_ingot", has(ingotItem))
            .save(pWriter, new ResourceLocation(SmeltingMetalMod.MODID, "smelting/molten_" + metalType + "_from_blasting"));
    }
}