package com.smeltingmetal.datagen;

import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.recipes.MoltenMetalSmeltingSerializer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        Map<String, MetalProperties> metals;
        try {
            metals = ModMetals.getAllMetalProperties();
            if (metals.isEmpty()) {
                LOGGER.warn("No metals found, using default recipes");
                addDefaultRecipes(pWriter);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get metal properties, using default recipes", e);
            addDefaultRecipes(pWriter);
            return;
        }

        for (MetalProperties metalProps : metals.values()) {
            addMetalRecipes(pWriter, metalProps.id(), metalProps.ingotId());
        }
    }
    
    private void addDefaultRecipes(Consumer<FinishedRecipe> pWriter) {
        // Add default recipes for common metals
        addMetalRecipes(pWriter, "iron", new ResourceLocation("minecraft", "iron_ingot"));
        addMetalRecipes(pWriter, "gold", new ResourceLocation("minecraft", "gold_ingot"));
        addMetalRecipes(pWriter, "copper", new ResourceLocation("minecraft", "copper_ingot"));
    }
    
    private void addMetalRecipes(Consumer<FinishedRecipe> pWriter, String metalType, ResourceLocation ingotId) {
        ResourceLocation rawOreId = new ResourceLocation(ingotId.getNamespace(), "raw_" + metalType);
        Item rawOre = ForgeRegistries.ITEMS.getValue(rawOreId);

        if (rawOre == null || rawOre == Items.AIR) {
            LOGGER.warn("No raw ore found for metal type '{}' at expected ID '{}'.", metalType, rawOreId);
            return;
        }

        // Get the molten metal item for this metal type
        Item moltenMetalItem = ForgeRegistries.ITEMS.getValue(
            new ResourceLocation(SmeltingMetalMod.MODID, "molten_" + metalType));
            
        if (moltenMetalItem == null || moltenMetalItem == Items.AIR) {
            LOGGER.warn("No molten metal item found for metal type '{}'", metalType);
            return;
        }
        
        // Get configured recipe types
        List<? extends String> recipeTypes = MetalsConfig.CONFIG.recipeTypes.get();
        
        // Create recipes for each configured type
        for (String recipeType : recipeTypes) {
            int cookingTime;
            float experience = 0.7f;
            
            // Set cooking time based on recipe type
            switch (recipeType.toLowerCase()) {
                case "smelting" -> cookingTime = 200;
                case "blasting" -> cookingTime = 100;
                case "smoking" -> cookingTime = 100;
                case "campfire_cooking" -> cookingTime = 600;
                default -> {
                    LOGGER.warn("Unknown recipe type: {}. Using default smelting time.", recipeType);
                    cookingTime = 200;
                }
            }
            
            pWriter.accept(new FinishedCookingRecipe(
                new ResourceLocation(SmeltingMetalMod.MODID, 
                    String.format("%s_molten_from_%s_%s", metalType, rawOreId.getPath(), recipeType)),
                "smeltingmetal",
                Ingredient.of(rawOre),
                moltenMetalItem,
                experience,
                cookingTime,
                RecipeCategory.MISC,
                MoltenMetalSmeltingSerializer.INSTANCE,
                metalType
            ));
        }
    }
}