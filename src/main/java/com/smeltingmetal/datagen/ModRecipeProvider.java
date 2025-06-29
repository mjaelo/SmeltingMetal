package com.smeltingmetal.datagen;

import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals; // Import ModMetals
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties; // Import MetalProperties
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items; // Keep for RAW_ORE_TO_METAL_TYPE if you still use it for other recipes, otherwise remove if only using ModMetals
import net.minecraft.world.item.crafting.Ingredient;
import com.smeltingmetal.recipes.MoltenMetalSmeltingSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // Iterate through all registered metal properties from your central ModMetals class
        for (MetalProperties metalProps : ModMetals.getAllMetalProperties().values()) {
            String metalType = metalProps.id();
            ResourceLocation ingotId = metalProps.ingotId(); // This is the resulting ingot

            // Determine the raw ore from the ingot ID. This is a common convention, but might need
            // customization if your raw ores don't follow the "raw_" + metalType convention exactly.
            // Example: minecraft:iron_ingot -> minecraft:raw_iron
            ResourceLocation rawOreId = new ResourceLocation(ingotId.getNamespace(), "raw_" + metalType);
            Item rawOre = ForgeRegistries.ITEMS.getValue(rawOreId); // Look up the actual Item object

            if (rawOre == null || rawOre == Items.AIR) {
                // Log a warning if no raw ore found for this metal type.
                // This means you either don't have a raw ore for this metal or its ID is different.
                LOGGER.warn("No raw ore found for metal type '{}' at expected ID '{}'. Skipping recipe generation.", metalType, rawOreId);
                continue; // Skip this metal if no raw ore is found.
            }

            // Custom Smelting Recipe
            pWriter.accept(new FinishedCookingRecipe(
                    // Generate a unique ID for the recipe, e.g., smeltingmetal:iron_molten_from_raw_iron_smelting
                    new ResourceLocation(SmeltingMetalMod.MODID, metalType + "_molten_from_" + rawOreId.getPath() + "_smelting"),
                    "smeltingmetal", // Group name
                    Ingredient.of(rawOre),
                    ModItems.MOLTEN_METAL.get(), // Still generic molten_metal item
                    0.7f, // Experience
                    200, // Cooking time
                    RecipeCategory.MISC, // Category
                    MoltenMetalSmeltingSerializer.INSTANCE, // Your custom serializer
                    metalType // Pass the metalType to the custom serializer
            ));

            // Custom Blasting Recipe
            pWriter.accept(new FinishedCookingRecipe(
                    // Generate a unique ID for the recipe, e.g., smeltingmetal:iron_molten_from_raw_iron_blasting
                    new ResourceLocation(SmeltingMetalMod.MODID, metalType + "_molten_from_" + rawOreId.getPath() + "_blasting"),
                    "smeltingmetal", // Group name
                    Ingredient.of(rawOre),
                    ModItems.MOLTEN_METAL.get(), // Still generic molten_metal item
                    0.7f, // Experience
                    100, // Cooking time
                    RecipeCategory.MISC, // Category
                    MoltenMetalSmeltingSerializer.INSTANCE, // Your custom serializer
                    metalType // Pass the metalType to the custom serializer
            ));
        }
    }
}