package com.smeltingmetal.datagen;

import com.smeltingmetal.ModItems;
import com.smeltingmetal.ModMetals;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import com.smeltingmetal.recipes.MoltenMetalSmeltingSerializer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;
import java.util.Map;

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

        // Smelting recipe
        pWriter.accept(new FinishedCookingRecipe(
                new ResourceLocation(SmeltingMetalMod.MODID, metalType + "_molten_from_" + rawOreId.getPath() + "_smelting"),
                "smeltingmetal",
                Ingredient.of(rawOre),
                ModItems.MOLTEN_METAL.get(),
                0.7f,
                200,
                RecipeCategory.MISC,
                MoltenMetalSmeltingSerializer.INSTANCE,
                metalType
        ));

        // Blasting recipe
        pWriter.accept(new FinishedCookingRecipe(
                new ResourceLocation(SmeltingMetalMod.MODID, metalType + "_molten_from_" + rawOreId.getPath() + "_blasting"),
                "smeltingmetal",
                Ingredient.of(rawOre),
                ModItems.MOLTEN_METAL.get(),
                0.7f,
                100,
                RecipeCategory.MISC,
                MoltenMetalSmeltingSerializer.INSTANCE,
                metalType
        ));
    }
}