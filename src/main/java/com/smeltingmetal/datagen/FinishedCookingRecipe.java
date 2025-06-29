package com.smeltingmetal.datagen;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

public record FinishedCookingRecipe(
        ResourceLocation id,        // Record component for ID
        String group,
        Ingredient ingredient,
        Item result,
        float experience,
        int cookingTime,
        RecipeCategory category,
        RecipeSerializer<?> serializer, // Record component for serializer
        String metalType
) implements FinishedRecipe {

    // --- CRUCIAL FIXES HERE ---
    // Explicitly implement the getId() method from FinishedRecipe
    @Override
    public ResourceLocation getId() { // This MUST be getId(), not id()
        return this.id; // Delegates to the record component 'id'
    }

    // Explicitly implement the getType() method from FinishedRecipe
    @Override
    public RecipeSerializer<?> getType() { // This MUST be getType(), not type()
        return this.serializer; // Delegates to the record component 'serializer'
    }
    // --- END CRUCIAL FIXES ---


    @Override
    public void serializeRecipeData(JsonObject pJson) {
        if (!this.group.isEmpty()) {
            pJson.addProperty("group", this.group);
        }
        pJson.add("ingredient", this.ingredient.toJson());
        pJson.addProperty("result", this.result.builtInRegistryHolder().key().location().toString());
        pJson.addProperty("experience", this.experience);
        pJson.addProperty("cookingtime", this.cookingTime);

        pJson.addProperty("category", this.category.getFolderName());

        pJson.addProperty("metalType", this.metalType);
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return null;
    }
}